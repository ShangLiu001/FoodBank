package com.primaryfeed;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Base class for integration tests using TestContainers MySQL.
 * Starts a MySQL container once for all tests and loads production SQL files
 * directly from src/main/sql (avoiding duplication).
 */
public abstract class AbstractTestContainersTest {

    private static final MySQLContainer<?> mysqlContainer;

    static {
        mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("primaryfeed")
                .withUsername("test")
                .withPassword("test")
                .withCommand("--log-bin-trust-function-creators=1");  // Allow triggers without SUPER privilege
                // Note: container reuse disabled to ensure clean state for each test run

        mysqlContainer.start();

        // Initialize database schema using production SQL files
        initializeDatabase();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    private static void initializeDatabase() {
        try {
            // Get JDBC connection to the container
            Connection connection = DriverManager.getConnection(
                    mysqlContainer.getJdbcUrl(),
                    mysqlContainer.getUsername(),
                    mysqlContainer.getPassword()
            );

            // Load and execute production SQL files from src/main/sql (avoids duplication)
            executeSqlFileFromPath(connection, "src/main/sql/dbDDL.sql");
            executeSqlFileFromPath(connection, "src/main/sql/dbTRIGGERS.sql");
            executeSqlFileFromPath(connection, "src/main/sql/dbDML.sql");

            // Load test-specific data from classpath
            executeSqlFileFromClasspath(connection, "/sql/testData.sql");

            connection.close();

            System.out.println("✓ TestContainers MySQL initialized with production schema and test data");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize test database", e);
        }
    }

    /**
     * Load and execute SQL file from filesystem path (e.g., src/main/sql/dbDDL.sql)
     */
    private static void executeSqlFileFromPath(Connection connection, String filePath) throws Exception {
        Path path = Paths.get(filePath);
        String sql = Files.readString(path);
        executeSql(connection, sql, filePath);
    }

    /**
     * Load and execute SQL file from classpath resources (e.g., /sql/testData.sql)
     */
    private static void executeSqlFileFromClasspath(Connection connection, String resourcePath) throws Exception {
        // Read SQL file from resources
        String sql = new BufferedReader(new InputStreamReader(
                AbstractTestContainersTest.class.getResourceAsStream(resourcePath)))
                .lines()
                .collect(Collectors.joining("\n"));
        executeSql(connection, sql, resourcePath);
    }

    /**
     * Parse and execute SQL with DELIMITER support
     */
    private static void executeSql(Connection connection, String sql, String sourcePath) throws Exception {

        // Remove inline comments (but keep the line structure for multi-line statements)
        StringBuilder cleanedSql = new StringBuilder();
        for (String line : sql.split("\n")) {
            // Remove inline comments
            int commentIndex = line.indexOf("--");
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex);
            }
            cleanedSql.append(line).append("\n");
        }
        sql = cleanedSql.toString();

        // Handle DELIMITER changes and split statements
        String currentDelimiter = ";";
        String[] parts = sql.split("\n");
        StringBuilder currentStatement = new StringBuilder();

        try (Statement stmt = connection.createStatement()) {
            for (String line : parts) {
                String trimmed = line.trim();

                // Handle DELIMITER command
                if (trimmed.toUpperCase().startsWith("DELIMITER ")) {
                    // Execute any pending statement before changing delimiter
                    if (currentStatement.length() > 0) {
                        executeStatement(stmt, currentStatement.toString().trim(), sourcePath);
                        currentStatement = new StringBuilder();
                    }
                    currentDelimiter = trimmed.substring(10).trim();
                    continue;
                }

                // Skip empty lines
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Skip special commands
                if (trimmed.toUpperCase().startsWith("USE ") ||
                    trimmed.toUpperCase().startsWith("SOURCE ") ||
                    (trimmed.toUpperCase().startsWith("SELECT ") && trimmed.contains("as message"))) {
                    continue;
                }

                // Accumulate statement
                if (currentStatement.length() > 0) {
                    currentStatement.append("\n");
                }
                currentStatement.append(line);

                // Check if this line ends with the current delimiter
                if (trimmed.endsWith(currentDelimiter)) {
                    // Remove delimiter and execute
                    String toExecute = currentStatement.toString().trim();
                    // Remove the delimiter from the end
                    if (currentDelimiter.equals(";")) {
                        toExecute = toExecute.substring(0, toExecute.length() - 1).trim();
                    } else {
                        // For $$ delimiter
                        toExecute = toExecute.substring(0, toExecute.lastIndexOf(currentDelimiter)).trim();
                    }

                    if (!toExecute.isEmpty()) {
                        executeStatement(stmt, toExecute, sourcePath);
                    }

                    currentStatement = new StringBuilder();
                }
            }

            // Execute any remaining statement
            if (currentStatement.length() > 0) {
                String toExecute = currentStatement.toString().trim();
                if (!toExecute.isEmpty()) {
                    executeStatement(stmt, toExecute, sourcePath);
                }
            }
        }
    }

    private static void executeStatement(Statement stmt, String sql, String sourcePath) {
        try {
            stmt.execute(sql);
        } catch (Exception e) {
            // Log but don't fail on individual statement errors
            if (!e.getMessage().contains("doesn't exist") &&
                !e.getMessage().contains("already exists")) {
                System.err.println("⚠ Warning executing SQL from " + sourcePath + ": " + e.getMessage());
                System.err.println("Statement preview: " + sql.substring(0, Math.min(100, sql.length())).replace("\n", " "));
            }
        }
    }
}
