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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and user authentication endpoints")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Operation(
        summary = "User login",
        description = "Authenticate with email and password. Returns JWT token and user info."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "role": "ROLE_STAFF",
                      "userId": 1,
                      "name": "Alice Nguyen"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "403", description = "Account inactive")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login credentials",
            required = true,
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "email": "alice.nguyen@bafb.org",
                      "password": "yourpassword"
                    }
                    """)
            )
        )
        @RequestBody Map<String, String> body) {
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

    @Operation(
        summary = "Get current user",
        description = "Returns the authenticated user's profile information",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile retrieved"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<?> me(
        @RequestHeader(value = "Authorization", required = false)
        @io.swagger.v3.oas.annotations.Parameter(hidden = true)
        String header
    ) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String token = header.substring(7);
        String email = jwtUtil.extractEmail(token);
        return userService.findByEmail(email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
