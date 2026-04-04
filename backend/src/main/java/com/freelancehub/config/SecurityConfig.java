package com.freelancehub.config;

import com.freelancehub.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration
 *
 * This class tells Spring Security:
 *   - Which endpoints are public (no token needed)
 *   - Which endpoints require authentication
 *   - To use JWT (stateless) instead of sessions
 *   - How to hash/verify passwords (BCrypt)
 *   - CORS settings (allow the frontend to call our API)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF - not needed for stateless JWT APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Enable CORS with our custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Define which routes need authentication
            .authorizeHttpRequests(auth -> auth
                // Public routes - no token needed
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/projects/**").permitAll()
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/pages/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )

            // Use stateless sessions - no server-side session storage
            // JWT tokens carry all the auth info
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Use our custom user details + password encoder
            .authenticationProvider(authenticationProvider())

            // Add our JWT filter BEFORE Spring's default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration - allows the frontend (running on a different port/domain)
     * to call our backend API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));          // in prod: set your domain
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * BCrypt password encoder.
     * BCrypt automatically salts and hashes passwords.
     * Strength 10 means 2^10 = 1024 hashing rounds (balances security and speed).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Authentication provider - tells Spring how to load users and verify passwords.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager is used in AuthService to authenticate login attempts.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
