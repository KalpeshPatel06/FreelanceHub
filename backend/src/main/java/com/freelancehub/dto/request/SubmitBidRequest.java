package com.freelancehub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitBidRequest {

    @NotBlank(message = "Proposal is required")
    @Size(min = 20, message = "Proposal must be at least 20 characters")
    private String proposal;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "1.00", message = "Bid amount must be at least $1")
    private BigDecimal bidAmount;
}
