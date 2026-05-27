package com.terimbere.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class IncomeSourceRequest {
    @NotBlank(message = "Source name is required")
    private String sourceName;

    @NotNull(message = "Expected amount is required")
    @Positive(message = "Expected amount must be greater than zero")
    private BigDecimal expectedAmount;

    private BigDecimal receivedAmount;

    @NotBlank(message = "Status is required")
    private String status; // PENDING, PARTIAL, RECEIVED
}
