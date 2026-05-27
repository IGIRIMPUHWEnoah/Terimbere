package com.terimbere.budget.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "budget_entries")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BudgetEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType; // ENUM: INCOME, EXPENSE

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 255)
    private String description;

    @Column(name = "planned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal plannedAmount;

    @Column(name = "actual_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;
}
