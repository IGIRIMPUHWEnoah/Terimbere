package com.terimbere.budget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "debt_records")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DebtRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "debt_direction", nullable = false, length = 20)
    private String debtDirection; // ENUM: THEY_OWE_ME, I_OWE_THEM

    @Column(name = "original_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "remaining_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    private String status; // ENUM: ACTIVE, PARTIALLY_PAID, PAID, OVERDUE

    @Builder.Default
    @Column(name = "scheduling_mode", length = 20)
    private String schedulingMode = "SINGLE"; // ENUM: SINGLE, SCHEDULED

    @Column(length = 20)
    private String frequency; // ENUM: WEEKLY, BI_WEEKLY, MONTHLY

    @Column(name = "number_of_installments")
    private Integer numberOfInstallments;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    public void validateAndUpdateState() {
        if (remainingAmount != null && originalAmount != null) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                remainingAmount = BigDecimal.ZERO;
                status = "PAID";
            } else if (remainingAmount.compareTo(originalAmount) > 0) {
                remainingAmount = originalAmount;
            }
        }
    }
}
