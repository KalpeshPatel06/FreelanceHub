package com.freelancehub.service;

import com.freelancehub.dto.request.LoginRequest;
import com.freelancehub.dto.request.RegisterRequest;
import com.freelancehub.dto.response.AuthResponse;
import com.freelancehub.entity.User;
import com.freelancehub.exception.BadRequestException;
import com.freelancehub.repository.UserRepository;
import com.freelancehub.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 *
 * @ExtendWith(MockitoExtension.class) enables Mockito annotations.
 *
 * What are mocks?
 *   Mocks are fake versions of dependencies. Instead of actually hitting
 *   the database or generating real JWT tokens, we control what these
 *   fake objects return. This makes tests fast and isolated.
 *
 * Pattern: Arrange → Act → Assert (AAA)
 *   Arrange: set up the test data and mock behavior
 *   Act:     call the method being tested
 *   Assert:  verify the result is what we expected
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock creates a fake version of each dependency
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    // @InjectMocks creates the real AuthService and injects the mocks above
    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        // Arrange: common test data used by multiple tests
        registerRequest = new RegisterRequest();
        registerRequest.setName("Alice Johnson");
        registerRequest.setEmail("alice@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(User.Role.CLIENT);

        savedUser = User.builder()
                .id(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .password("$2a$10$hashedPassword")
                .role(User.Role.CLIENT)
                .build();
    }

    // ====================================================================
    // Registration Tests
    // ====================================================================

    @Test
    @DisplayName("register() - success: saves user and returns token")
    void register_success() {
        // Arrange: email is NOT already taken
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(mockUserDetails)).thenReturn("mocked-jwt-token");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo(User.Role.CLIENT);
        assertThat(response.getName()).isEqualTo("Alice Johnson");

        // Verify the user was actually saved
        verify(userRepository, times(1)).save(any(User.class));
        // Verify password was hashed (never stored plain)
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("register() - failure: throws BadRequestException when email already exists")
    void register_emailAlreadyExists_throwsBadRequestException() {
        // Arrange: email IS already taken
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // Act & Assert: expect the exception
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        // Verify we never tried to save a duplicate user
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() - password is hashed before saving")
    void register_passwordIsHashed() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            // Assert password is NOT stored plain
            assertThat(u.getPassword()).isEqualTo("bcrypt-hash");
            assertThat(u.getPassword()).doesNotContain("password123");
            return savedUser;
        });

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
    }

    // ====================================================================
    // Login Tests
    // ====================================================================

    @Test
    @DisplayName("login() - success: returns token for valid credentials")
    void login_success() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("password123");

        // authenticationManager.authenticate() succeeds (no exception = success)
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("alice@example.com", "password123")
        );
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(mockUserDetails)).thenReturn("login-jwt-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response.getToken()).isEqualTo("login-jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("login() - failure: throws exception for wrong password")
    void login_wrongPassword_throwsException() {
        // Arrange: authenticationManager throws on bad credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
