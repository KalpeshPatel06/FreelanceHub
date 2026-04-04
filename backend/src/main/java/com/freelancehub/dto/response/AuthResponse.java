package com.freelancehub.dto.response;

import com.freelancehub.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned to the client after successful login or registration.
 * Contains the JWT token and basic user info.
 * The frontend stores the token and sends it with every request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private User.Role role;
    private String profileImageUrl;
}
