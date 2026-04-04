package com.freelancehub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API response envelope.
 *
 * Every API endpoint returns this shape, making the frontend's job easier.
 * Example success response:
 * {
 *   "success": true,
 *   "message": "Project created successfully",
 *   "data": { ... },
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * Example error response:
 * {
 *   "success": false,
 *   "message": "Project not found",
 *   "data": null,
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * @JsonInclude(NON_NULL) - omits null fields from JSON output (keeps responses clean)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ---- Static factory helpers ----

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Success", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
