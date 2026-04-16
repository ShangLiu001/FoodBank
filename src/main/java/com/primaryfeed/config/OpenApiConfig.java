package com.primaryfeed.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for PrimaryFeed API documentation.
 * Accessible at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI primaryFeedOpenAPI() {
        // Define the JWT security scheme
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("PrimaryFeed API")
                        .description("""
                                Food Bank Management System API

                                ## Authentication
                                Most endpoints require JWT authentication.
                                1. Call `POST /api/auth/login` with valid credentials
                                2. Copy the returned token
                                3. Click the 'Authorize' button and enter: `Bearer <your-token>`

                                ## Roles
                                - **STAFF**: Full access to all endpoints including reports
                                - **VOLUNTEER**: Access to operational endpoints (inventory, donations, distributions)

                                ## Database Schema
                                The system manages:
                                - Food banks & branches
                                - Inventory with expiry tracking
                                - Donations from donors
                                - Distributions to beneficiaries
                                - Volunteer scheduling
                                - 17 insight queries for reporting
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("The Primary Keys - Group 6")
                                .email("support@primaryfeed.org")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("http://34.10.48.147:8080")
                                .description("Production server (GCP)")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token obtained from /api/auth/login")));
    }
}
