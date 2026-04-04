package com.freelancehub.dto.request;

import com.freelancehub.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Data Transfer Object for user registration.
 * This is what the JSON request body maps to.
 * Using a DTO (not the entity) keeps validation logic out of the entity
 * and prevents exposing internal fields to the API.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    private User.Role role;
}
