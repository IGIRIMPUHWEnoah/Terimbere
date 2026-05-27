package com.terimbere.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IncomePlanRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    @NotNull(message = "Target date is required")
    private LocalDate targetDate;

    private String strategyNotes;

    @NotBlank(message = "Status is required")
    private String status; // DRAFT, ACTIVE, ACHIEVED, ABANDONED
}
