package com.freelancehub.controller;

import com.freelancehub.dto.request.LoginRequest;
import com.freelancehub.dto.request.RegisterRequest;
import com.freelancehub.dto.response.ApiResponse;
import com.freelancehub.dto.response.AuthResponse;
import com.freelancehub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - handles user registration and login HTTP requests.
 *
 * @RestController = @Controller + @ResponseBody
 *   Means every method's return value is automatically serialized to JSON.
 *
 * @RequestMapping("/api/auth") - all endpoints in this class start with /api/auth
 *
 * These endpoints are PUBLIC (no token required) - configured in SecurityConfig.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     *
     * Register a new user account.
     *
     * Request body example:
     * {
     *   "name": "Alice Johnson",
     *   "email": "alice@example.com",
     *   "password": "securePass123",
     *   "role": "CLIENT"
     * }
     *
     * @Valid triggers the validation annotations on RegisterRequest.
     * If validation fails, Spring automatically returns 400 Bad Request.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse authResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authResponse));
    }

    /**
     * POST /api/auth/login
     *
     * Login with email and password. Returns a JWT token.
     *
     * Request body example:
     * {
     *   "email": "alice@example.com",
     *   "password": "securePass123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}
