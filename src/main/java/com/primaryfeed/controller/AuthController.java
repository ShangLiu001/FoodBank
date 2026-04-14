package com.primaryfeed.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.primaryfeed.auth.JwtUtil;
import com.primaryfeed.entity.User;
import com.primaryfeed.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /** POST /api/auth/login — Body: { "email": "...", "password": "..." } */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");

        User user = userService.findByEmail(email).orElse(null);
        System.out.println("=== LOGIN DEBUG ===");
        System.out.println("Email: " + email);
        System.out.println("User found: " + (user != null));
        if (user != null) {
            System.out.println("Hash in DB: " + user.getPasswordHash());
            System.out.println("Password match: " + passwordEncoder.matches(password, user.getPasswordHash()));
        }
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        if (user.getStatus() == 0) {
            return ResponseEntity.status(403).body(Map.of("error", "Account inactive"));
        }

        String token = jwtUtil.generateToken(email, user.getRoleName());
        return ResponseEntity.ok(Map.of(
                "token",  token,
                "role",   user.getRoleName(),
                "userId", user.getUserId(),
                "name",   user.getFirstName() + " " + user.getLastName()
        ));
    }

    /** GET /api/auth/me — Returns the authenticated user's profile. */
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String header) {
        String token = header.substring(7);
        String email = jwtUtil.extractEmail(token);
        return userService.findByEmail(email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
