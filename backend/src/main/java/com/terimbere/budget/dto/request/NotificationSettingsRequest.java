package com.terimbere.budget.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationSettingsRequest {
    @NotNull(message = "Email notifications toggle is required")
    private Boolean emailNotifications;

    @NotNull(message = "In-app notifications toggle is required")
    private Boolean inAppNotifications;

    @NotNull(message = "Days before bill reminder is required")
    @Min(value = 1, message = "Must trigger at least 1 day before")
    @Max(value = 30, message = "Cannot trigger more than 30 days before")
    private Integer daysBeforeBillReminder;

    @NotNull(message = "Days before contract expiry is required")
    @Min(value = 1, message = "Must trigger at least 1 day before")
    @Max(value = 30, message = "Cannot trigger more than 30 days before")
    private Integer daysBeforeContractExpiry;
}
