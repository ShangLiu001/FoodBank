package com.primaryfeed.auth;

import com.primaryfeed.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationFlowTest extends BaseIntegrationTest {

    @Test
    void testLoginWithStaffCredentials_ReturnsJwtWithStaffRole() {
        // Arrange
        Map<String, String> credentials = Map.of(
                "email", "staff@test.com",
                "password", "test123"
        );

        // Act
        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login",
                request,
                Map.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("token"));
        assertEquals("ROLE_STAFF", response.getBody().get("role"));
        assertNotNull(response.getBody().get("userId"));
        assertNotNull(response.getBody().get("name"));
    }

    @Test
    void testLoginWithInvalidCredentials_Returns401() {
        // Arrange
        Map<String, String> credentials = Map.of(
                "email", "staff@test.com",
                "password", "wrongpassword"
        );

        // Act
        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login",
                request,
                Map.class
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        assertEquals("Invalid credentials", response.getBody().get("error"));
    }

    @Test
    void testLoginWithInactiveUser_Returns403() {
        // Arrange
        Map<String, String> credentials = Map.of(
                "email", "inactive@test.com",
                "password", "test123"
        );

        // Act
        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login",
                request,
                Map.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        assertEquals("Account inactive", response.getBody().get("error"));
    }
}
