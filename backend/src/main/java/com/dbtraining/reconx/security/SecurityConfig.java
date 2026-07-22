package com.dbtraining.reconx.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 *
 * Day-1: permit all endpoints so frontend/swagger can load.
 * Later ADV073/ADV074:
 * - JWT authentication
 * - RBAC roles
 * - JwtAuthenticationFilter
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Password hashing for user authentication.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Main Spring Security filter chain.
     *
     * Current development mode:
     * - CSRF disabled
     * - H2 console allowed
     * - All endpoints open
     *
     * Replace with JWT rules for ADV073/ADV074.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())

                // allow H2 console in dev
                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())
                )

                // Day-1: everything accessible
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll()
                )

                .build();
    }
}
