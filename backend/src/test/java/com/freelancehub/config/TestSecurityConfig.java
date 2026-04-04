package com.freelancehub.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TestSecurityConfig - used only in @WebMvcTest tests.
 *
 * @WebMvcTest loads the full security stack by default, which makes
 * controller tests very verbose (you have to mock JWT validation, etc.).
 *
 * This config replaces the real SecurityConfig for tests and simply
 * permits all requests — so tests focus on controller logic only.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
