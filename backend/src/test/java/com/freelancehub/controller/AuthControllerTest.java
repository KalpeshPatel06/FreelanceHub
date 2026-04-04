package com.freelancehub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelancehub.config.TestSecurityConfig;
import com.freelancehub.dto.request.RegisterRequest;
import com.freelancehub.dto.response.AuthResponse;
import com.freelancehub.entity.User;
import com.freelancehub.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer integration test using MockMvc.
 *
 * @WebMvcTest loads only the web layer (controllers, security config).
 * It does NOT start the full Spring context or connect to a database.
 * Dependencies (like AuthService) are replaced with @MockBean fakes.
 *
 * MockMvc lets us simulate HTTP requests without starting a real server.
 * TestSecurityConfig disables JWT auth so tests stay focused on controller logic.
 */
@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;      // simulates HTTP requests

    @Autowired
    private ObjectMapper objectMapper;  // converts objects to/from JSON

    @MockBean
    private AuthService authService;  // fake service - we control its behavior

    @Test
    @DisplayName("POST /api/auth/register - returns 201 with token on success")
    void register_validRequest_returns201() throws Exception {
        // Arrange: build the request body
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

        // Act & Assert: simulate POST request and verify response
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())                          // HTTP 201
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.role").value("CLIENT"));
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 for invalid email")
    void register_invalidEmail_returns400() throws Exception {
        // Arrange: email is invalid
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("not-an-email");   // invalid!
        request.setPassword("password123");
        request.setRole(User.Role.CLIENT);

        // Act & Assert: validation should fail with 400 before even reaching the service
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").exists()); // validation error for email field
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password too short")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("short");  // less than 8 chars
        request.setRole(User.Role.CLIENT);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").exists());
    }
}
