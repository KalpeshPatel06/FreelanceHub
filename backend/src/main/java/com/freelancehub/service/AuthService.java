package com.freelancehub.service;

import com.freelancehub.dto.request.LoginRequest;
import com.freelancehub.dto.request.RegisterRequest;
import com.freelancehub.dto.response.AuthResponse;
import com.freelancehub.entity.User;
import com.freelancehub.exception.BadRequestException;
import com.freelancehub.repository.UserRepository;
import com.freelancehub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService - business logic for registration and login.
 *
 * Service layer rules:
 *   - Contains all business logic (validation, processing)
 *   - Calls repositories to interact with the database
 *   - Never directly handles HTTP requests/responses (that's the controller's job)
 *   - @Transactional ensures database operations are atomic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Register a new user.
     *
     * Steps:
     *   1. Check email isn't already taken
     *   2. Hash the password with BCrypt
     *   3. Save the user to the database
     *   4. Generate a JWT token
     *   5. Return the token + user info
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Step 1: Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }

        // Step 2 & 3: Build and save the user entity
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} ({})", savedUser.getEmail(), savedUser.getRole());

        // Step 4 & 5: Generate token and return response
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return buildAuthResponse(token, savedUser);
    }

    /**
     * Login an existing user.
     *
     * Steps:
     *   1. Let Spring Security verify the email + password
     *      (throws an exception automatically if credentials are wrong)
     *   2. Load the full user from DB
     *   3. Generate a JWT token
     *   4. Return the token + user info
     */
    public AuthResponse login(LoginRequest request) {
        // Step 1: Authenticate - Spring verifies email/password against the database
        // If wrong: AuthenticationException is thrown automatically
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Step 2: Load user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        log.info("User logged in: {}", user.getEmail());

        // Steps 3 & 4: Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    /** Build the AuthResponse DTO from a token and user. */
    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
