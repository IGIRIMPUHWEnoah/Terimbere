package com.terimbere.budget.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DebtPaymentRequest {
    @NotNull(message = "Amount paid is required")
    @Positive(message = "Amount paid must be greater than zero")
    private BigDecimal amountPaid;

    private String paymentMethod; // e.g. Mobile Money, Cash, Bank Transfer
    private String notes;
}
