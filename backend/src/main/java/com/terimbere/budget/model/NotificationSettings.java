package com.terimbere.budget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "notification_settings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationSettings {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "email_notifications")
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(name = "in_app_notifications")
    @Builder.Default
    private Boolean inAppNotifications = true;

    @Column(name = "days_before_bill_reminder")
    @Builder.Default
    private Integer daysBeforeBillReminder = 3;

    @Column(name = "days_before_contract_expiry")
    @Builder.Default
    private Integer daysBeforeContractExpiry = 7;

    @Column(name = "debt_check_hour")
    @Builder.Default
    private Integer debtCheckHour = 0;

    @Column(name = "debt_check_minute")
    @Builder.Default
    private Integer debtCheckMinute = 0;
}
