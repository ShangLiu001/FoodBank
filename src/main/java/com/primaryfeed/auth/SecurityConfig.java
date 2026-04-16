package com.primaryfeed.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers("/", "/health", "/api/auth/**").permitAll()
                        // Swagger/OpenAPI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Staff only
                        .requestMatchers("/api/reports/**").hasRole("STAFF")
                        .requestMatchers("/api/users/**").hasRole("STAFF")
                        // All authenticated users (staff + volunteers)
                        .requestMatchers("/api/inventory/**").hasAnyRole("STAFF", "VOLUNTEER")
                        .requestMatchers("/api/donations/**").hasAnyRole("STAFF", "VOLUNTEER")
                        .requestMatchers("/api/donation-items/**").hasAnyRole("STAFF", "VOLUNTEER")
                        .requestMatchers("/api/distributions/**").hasAnyRole("STAFF", "VOLUNTEER")
                        .requestMatchers("/api/distribution-items/**").hasAnyRole("STAFF", "VOLUNTEER")
                        .requestMatchers("/api/beneficiaries/**").hasAnyRole("STAFF", "VOLUNTEER")
                        .requestMatchers("/api/donors/**").hasAnyRole("STAFF", "VOLUNTEER")
                        .requestMatchers("/api/volunteers/**").hasAnyRole("STAFF", "VOLUNTEER")
                        .requestMatchers("/api/shifts/**").hasAnyRole("STAFF", "VOLUNTEER")
                        // Fallback
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
