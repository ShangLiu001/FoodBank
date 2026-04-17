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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Static files (frontend)
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/favicon.ico", "/icons.svg").permitAll()
                        // Allow React Router paths (SpaController forwards these)
                        .requestMatchers("/dashboard", "/inventory", "/operations", "/community", "/reports", "/login").permitAll()
                        // Public API
                        .requestMatchers("/health", "/api/auth/**").permitAll()
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:3000",   // fallback
                "http://localhost:4173"    // Vite preview
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
