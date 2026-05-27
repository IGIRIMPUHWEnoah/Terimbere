package com.terimbere.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BudgetEntryRequest {
    @NotBlank(message = "Entry type is required")
    private String entryType; // INCOME, EXPENSE

    @NotBlank(message = "Category is required")
    private String category;

    private String description;

    @NotNull(message = "Planned amount is required")
    @PositiveOrZero(message = "Planned amount must be positive or zero")
    private BigDecimal plannedAmount;

    @PositiveOrZero(message = "Actual amount must be positive or zero")
    private BigDecimal actualAmount;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;
}
