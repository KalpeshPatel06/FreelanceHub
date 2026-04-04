package com.freelancehub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be 5-200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, message = "Description must be at least 20 characters")
    private String description;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "1.00", message = "Budget must be at least $1")
    private BigDecimal budget;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    private LocalDate deadline;
}
