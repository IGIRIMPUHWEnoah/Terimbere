package com.terimbere.budget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "debt_installments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DebtInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_record_id", nullable = false)
    private DebtRecord debtRecord;

    @Column(name = "amount_expected", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountExpected;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    private String status; // ENUM: PENDING, PAID, OVERDUE

    @PrePersist
    @PreUpdate
    public void validateAndUpdateState() {
        if (amountPaid != null && amountExpected != null) {
            if (amountPaid.compareTo(amountExpected) >= 0) {
                amountPaid = amountExpected;
                status = "PAID";
            }
        }
    }
}
