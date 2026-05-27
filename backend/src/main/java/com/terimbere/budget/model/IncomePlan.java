package com.terimbere.budget.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "income_plans")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IncomePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "strategy_notes", columnDefinition = "TEXT")
    private String strategyNotes;

    @Column(nullable = false, length = 20)
    private String status; // ENUM: DRAFT, ACTIVE, ACHIEVED, ABANDONED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "incomePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IncomeSource> sources = new ArrayList<>();

    public void addSource(IncomeSource source) {
        sources.add(source);
        source.setIncomePlan(this);
    }

    public void removeSource(IncomeSource source) {
        sources.remove(source);
        source.setIncomePlan(null);
    }
}
