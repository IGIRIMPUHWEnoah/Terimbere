package com.terimbere.budget.controller;

import com.terimbere.budget.dto.request.BudgetEntryRequest;
import com.terimbere.budget.dto.request.BudgetRequest;
import com.terimbere.budget.model.Budget;
import com.terimbere.budget.model.BudgetEntry;
import com.terimbere.budget.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Budgeting Engine", description = "Endpoints for managing named budget books and allocating individual income or expense entries.")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "Get all budgets for current user")
    public ResponseEntity<List<Budget>> getAllBudgets() {
        return ResponseEntity.ok(budgetService.getAllBudgetsForCurrentUser());
    }

    @GetMapping("/{budgetId}")
    @Operation(summary = "Get a single budget by ID")
    public ResponseEntity<Budget> getBudgetById(@PathVariable UUID budgetId) {
        return ResponseEntity.ok(budgetService.getBudgetById(budgetId));
    }

    @PostMapping
    @Operation(summary = "Create a new budget book")
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody BudgetRequest request) {
        Budget budget = budgetService.createBudget(request);
        return new ResponseEntity<>(budget, HttpStatus.CREATED);
    }

    @PutMapping("/{budgetId}")
    @Operation(summary = "Update an existing budget book details")
    public ResponseEntity<Budget> updateBudget(@PathVariable UUID budgetId, @Valid @RequestBody BudgetRequest request) {
        Budget budget = budgetService.updateBudget(budgetId, request);
        return ResponseEntity.ok(budget);
    }

    @DeleteMapping("/{budgetId}")
    @Operation(summary = "Delete an entire budget book and its allocations")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID budgetId) {
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{budgetId}/entries")
    @Operation(summary = "Allocate a new entry inside a budget")
    public ResponseEntity<BudgetEntry> addEntryToBudget(@PathVariable UUID budgetId, @Valid @RequestBody BudgetEntryRequest request) {
        BudgetEntry entry = budgetService.addEntryToBudget(budgetId, request);
        return new ResponseEntity<>(entry, HttpStatus.CREATED);
    }

    @PutMapping("/{budgetId}/entries/{entryId}")
    @Operation(summary = "Update an allocated budget entry")
    public ResponseEntity<BudgetEntry> updateBudgetEntry(@PathVariable UUID budgetId, @PathVariable UUID entryId, @Valid @RequestBody BudgetEntryRequest request) {
        BudgetEntry entry = budgetService.updateBudgetEntry(budgetId, entryId, request);
        return ResponseEntity.ok(entry);
    }

    @DeleteMapping("/{budgetId}/entries/{entryId}")
    @Operation(summary = "Deallocate an entry from a budget")
    public ResponseEntity<Void> removeEntryFromBudget(@PathVariable UUID budgetId, @PathVariable UUID entryId) {
        budgetService.removeEntryFromBudget(budgetId, entryId);
        return ResponseEntity.noContent().build();
    }
}
