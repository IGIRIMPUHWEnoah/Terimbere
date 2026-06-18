package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.BudgetEntryRequest;
import com.terimbere.budget.dto.request.BudgetRequest;
import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.Budget;
import com.terimbere.budget.model.BudgetEntry;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.BudgetEntryRepository;
import com.terimbere.budget.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetEntryRepository budgetEntryRepository;
    private final AuthService authService;

    public BudgetService(BudgetRepository budgetRepository,
                         BudgetEntryRepository budgetEntryRepository,
                         AuthService authService) {
        this.budgetRepository = budgetRepository;
        this.budgetEntryRepository = budgetEntryRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<Budget> getAllBudgetsForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return budgetRepository.findByUserWithEntries(user);
    }

    @Transactional(readOnly = true)
    public Budget getBudgetById(UUID budgetId) {
        User user = authService.getCurrentAuthenticatedUser();
        Budget budget = budgetRepository.findByIdWithEntries(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to budget");
        }
        return budget;
    }

    @Transactional
    public Budget createBudget(BudgetRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        Budget budget = Budget.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .periodType(request.getPeriodType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .budgetType(request.getBudgetType() != null ? request.getBudgetType() : "PERSONAL")
                .notes(request.getNotes())
                .savingsGoal(request.getSavingsGoal())
                .projectTotalBudget(request.getProjectTotalBudget())
                .build();
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget updateBudget(UUID budgetId, BudgetRequest request) {
        Budget budget = getBudgetById(budgetId);
        budget.setName(request.getName());
        budget.setDescription(request.getDescription());
        budget.setPeriodType(request.getPeriodType());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setStatus(request.getStatus());
        if (request.getBudgetType() != null) {
            budget.setBudgetType(request.getBudgetType());
        }
        budget.setNotes(request.getNotes());
        budget.setSavingsGoal(request.getSavingsGoal());
        budget.setProjectTotalBudget(request.getProjectTotalBudget());
        return budgetRepository.save(budget);
    }

    @Transactional
    public void deleteBudget(UUID budgetId) {
        Budget budget = getBudgetById(budgetId);
        budgetRepository.delete(budget);
    }

    @Transactional
    public BudgetEntry addEntryToBudget(UUID budgetId, BudgetEntryRequest request) {
        Budget budget = getBudgetById(budgetId);
        BudgetEntry entry = BudgetEntry.builder()
                .entryType(request.getEntryType())
                .category(request.getCategory())
                .description(request.getDescription())
                .plannedAmount(request.getPlannedAmount())
                .actualAmount(request.getActualAmount() != null ? request.getActualAmount() : BigDecimal.ZERO)
                .entryDate(request.getEntryDate())
                .amountSaved(request.getAmountSaved())
                .build();
        budget.addEntry(entry);
        budgetRepository.save(budget);
        return entry;
    }

    @Transactional
    public BudgetEntry updateBudgetEntry(UUID budgetId, UUID entryId, BudgetEntryRequest request) {
        Budget budget = getBudgetById(budgetId);
        BudgetEntry entry = budgetEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget entry not found"));
        
        if (!entry.getBudget().getId().equals(budget.getId())) {
            throw new IllegalArgumentException("Budget entry does not belong to this budget");
        }

        entry.setEntryType(request.getEntryType());
        entry.setCategory(request.getCategory());
        entry.setDescription(request.getDescription());
        entry.setPlannedAmount(request.getPlannedAmount());
        if (request.getActualAmount() != null) {
            entry.setActualAmount(request.getActualAmount());
        }
        entry.setEntryDate(request.getEntryDate());
        entry.setAmountSaved(request.getAmountSaved());

        return budgetEntryRepository.save(entry);
    }

    @Transactional
    public void removeEntryFromBudget(UUID budgetId, UUID entryId) {
        Budget budget = getBudgetById(budgetId);
        BudgetEntry entry = budgetEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget entry not found"));

        if (!entry.getBudget().getId().equals(budget.getId())) {
            throw new IllegalArgumentException("Budget entry does not belong to this budget");
        }

        budget.removeEntry(entry);
        budgetRepository.save(budget);
    }
}
