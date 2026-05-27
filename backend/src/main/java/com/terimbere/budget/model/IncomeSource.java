package com.terimbere.budget.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "income_sources")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IncomeSource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_plan_id", nullable = false)
    private IncomePlan incomePlan;

    @Column(name = "source_name", nullable = false, length = 150)
    private String sourceName;

    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "received_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status; // ENUM: PENDING, PARTIAL, RECEIVED
}
