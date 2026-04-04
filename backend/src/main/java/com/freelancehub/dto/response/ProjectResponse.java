package com.freelancehub.dto.response;

import com.freelancehub.entity.Project;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProjectResponse - the shape of project data sent to the frontend.
 *
 * We never return the raw Project entity directly from controllers.
 * Reasons:
 *   1. Entities have lazy-loaded relationships that cause JSON serialization errors.
 *   2. We may want to include computed fields (e.g. bidCount).
 *   3. We control exactly what data the client sees.
 */
@Data
@Builder
public class ProjectResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal budget;
    private LocalDate deadline;
    private Project.Status status;
    private LocalDateTime createdAt;

    // Flattened client info (no need to send the entire User object)
    private Long clientId;
    private String clientName;

    // Computed field - how many bids this project has
    private long bidCount;

    /**
     * Static factory method - converts a Project entity to a ProjectResponse DTO.
     * This keeps conversion logic in one place.
     */
    public static ProjectResponse from(Project project, long bidCount) {
        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .budget(project.getBudget())
                .deadline(project.getDeadline())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .clientId(project.getClient().getId())
                .clientName(project.getClient().getName())
                .bidCount(bidCount)
                .build();
    }
}
