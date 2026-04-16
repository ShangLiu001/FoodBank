package com.primaryfeed.auth;

import com.primaryfeed.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorizationTest extends BaseIntegrationTest {

    @Test
    void testVolunteerCannotAccessReports_Returns403() {
        // Arrange
        HttpEntity<String> request = new HttpEntity<>(volunteerHeaders());

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reports/1?branchId=1",
                HttpMethod.GET,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testStaffCanAccessReports_Returns200() {
        // Arrange
        HttpEntity<String> request = new HttpEntity<>(staffHeaders());

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reports/1?branchId=1",
                HttpMethod.GET,
                request,
                String.class
        );

        // Assert
        // Should return 200 or possibly 204 if no data
        // We don't care about the exact data, just that it's authorized
        assertTrue(
                response.getStatusCode() == HttpStatus.OK ||
                response.getStatusCode() == HttpStatus.NO_CONTENT,
                "Staff should be able to access reports, but got " + response.getStatusCode()
        );
    }

    @Test
    void testUnauthenticatedRequest_Returns401() {
        // Arrange - no Authorization header

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reports/1?branchId=1",
                HttpMethod.GET,
                null,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
