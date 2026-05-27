package com.terimbere.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;
    private String email;
    private String address;

    @NotBlank(message = "Contact type is required")
    private String contactType; // DEBTOR, CREDITOR, BOTH

    private String notes;
}
