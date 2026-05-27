package com.terimbere.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private Boolean isRecurring;

    @NotBlank(message = "Recurrence period is required")
    private String recurrencePeriod; // ONCE, WEEKLY, MONTHLY, YEARLY

    @NotBlank(message = "Status is required")
    private String status; // UNPAID, PAID, OVERDUE

    private String notes;
}
