package com.primaryfeed.util;

import com.primaryfeed.AbstractTestContainersTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class BcryptHashGenerator extends AbstractTestContainersTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void generateHash() {
        String password = "test123";
        String hash = passwordEncoder.encode(password);
        System.out.println("=".repeat(80));
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("Verify: " + passwordEncoder.matches(password, hash));
        System.out.println("=".repeat(80));
    }
}
