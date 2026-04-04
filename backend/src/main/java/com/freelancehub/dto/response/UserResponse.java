package com.freelancehub.dto.response;

import com.freelancehub.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDateTime;

@Data        // generates getters, setters, equals, hashCode, toString
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private User.Role role;
    private String profileImageUrl;
    private LocalDateTime createdAt;

    // Stats shown on the profile page
    private long projectCount;   // for clients: projects posted
    private long bidCount;       // for freelancers: bids submitted

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
