package com.primaryfeed;

import com.primaryfeed.auth.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JwtUtil jwtUtil;

    protected String staffToken;
    protected String volunteerToken;

    @BeforeEach
    void setupTokens() {
        staffToken = "Bearer " + jwtUtil.generateToken("staff@test.com", "ROLE_STAFF");
        volunteerToken = "Bearer " + jwtUtil.generateToken("volunteer@test.com", "ROLE_VOLUNTEER");
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected HttpHeaders staffHeaders() {
        return authHeaders(staffToken);
    }

    protected HttpHeaders volunteerHeaders() {
        return authHeaders(volunteerToken);
    }
}
