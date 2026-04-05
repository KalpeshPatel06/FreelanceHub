package com.freelancehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelancehub.dto.request.RegisterRequest;
import com.freelancehub.dto.response.ApiResponse;
import com.freelancehub.dto.response.AuthResponse;
import com.freelancehub.entity.User;
import com.freelancehub.exception.GlobalExceptionHandler;
import com.freelancehub.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthControllerTest - tests the AuthController in complete isolation.
 *
 * Uses MockMvcBuilders.standaloneSetup() which builds a minimal Spring MVC
 * context with ONLY the AuthController and GlobalExceptionHandler.
 * No security filter chain, no JWT, no database — pure controller logic only.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    // Real controller with a mocked service injected
    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Build a standalone MockMvc with ONLY our controller and exception handler.
        // This completely bypasses Spring Security, JWT filters, and all other beans.
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 201 with token on success")
    void register_validRequest_returns201() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Johnson");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setRole(User.Role.CLIENT);

        AuthResponse mockResponse = AuthResponse.builder()
                .token("mock-jwt-token")
                .tokenType("Bearer")
                .userId(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .role(User.Role.CLIENT)
                .build();

        when(authService.register(any())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.role").value("CLIENT"));
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 for invalid email")
    void register_invalidEmail_returns400() throws Exception {
        // Arrange - invalid email format
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("not-an-email");
        request.setPassword("password123");
        request.setRole(User.Role.CLIENT);

        // Act & Assert - validation should reject this with 400
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password too short")
    void register_shortPassword_returns400() throws Exception {
        // Arrange - password less than 8 characters
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("short");
        request.setRole(User.Role.CLIENT);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}