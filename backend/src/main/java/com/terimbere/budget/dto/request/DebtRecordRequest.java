package com.terimbere.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class DebtRecordRequest {
    @NotNull(message = "Contact ID is required")
    private UUID contactId;

    @NotBlank(message = "Debt direction is required")
    private String debtDirection; // THEY_OWE_ME, I_OWE_THEM

    @NotNull(message = "Original amount is required")
    @Positive(message = "Original amount must be greater than zero")
    private BigDecimal originalAmount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String status; // ACTIVE, PARTIALLY_PAID, PAID, OVERDUE
}
