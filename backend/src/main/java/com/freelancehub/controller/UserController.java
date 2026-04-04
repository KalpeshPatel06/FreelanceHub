package com.freelancehub.controller;

import com.freelancehub.dto.response.ApiResponse;
import com.freelancehub.dto.response.UserResponse;
import com.freelancehub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * UserController - user profile and image upload endpoints.
 *
 * Endpoints:
 *   GET  /api/users/me                - get current user profile
 *   POST /api/users/me/profile-image  - upload profile image to S3
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     * Returns the logged-in user's profile info.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * POST /api/users/me/profile-image
     * Upload a profile image to AWS S3.
     *
     * Uses multipart/form-data (not JSON) because we're sending a binary file.
     * The 'file' parameter name must match the form field name in the frontend.
     *
     * Example curl:
     *   curl -X POST http://localhost:8080/api/users/me/profile-image \
     *        -H "Authorization: Bearer <token>" \
     *        -F "file=@/path/to/image.jpg"
     */
    @PostMapping(value = "/me/profile-image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        UserResponse user = userService.uploadProfileImage(file);
        return ResponseEntity.ok(ApiResponse.success("Profile image updated", user));
    }
}
