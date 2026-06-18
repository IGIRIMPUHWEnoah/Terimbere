package com.terimbere.budget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "budgets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "period_type", nullable = false, length = 30)
    private String periodType; // ENUM: MONTHLY, QUARTERLY, YEARLY, CUSTOM

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "budget_type", nullable = false, length = 20)
    @Builder.Default
    private String budgetType = "PERSONAL"; // PERSONAL, BUSINESS, PROJECT, SAVINGS, FAMILY

    @Column(nullable = false, length = 20)
    private String status; // ENUM: ACTIVE, ARCHIVED, DRAFT

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "savings_goal", precision = 15, scale = 2)
    private BigDecimal savingsGoal;

    @Column(name = "project_total_budget", precision = 15, scale = 2)
    private BigDecimal projectTotalBudget;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BudgetEntry> entries = new ArrayList<>();

    public void addEntry(BudgetEntry entry) {
        entries.add(entry);
        entry.setBudget(this);
    }

    public void removeEntry(BudgetEntry entry) {
        entries.remove(entry);
        entry.setBudget(null);
    }
}
