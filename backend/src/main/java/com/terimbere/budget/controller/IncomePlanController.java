package com.terimbere.budget.controller;

import com.terimbere.budget.dto.request.IncomePlanRequest;
import com.terimbere.budget.dto.request.IncomeSourceRequest;
import com.terimbere.budget.model.IncomePlan;
import com.terimbere.budget.model.IncomeSource;
import com.terimbere.budget.service.IncomeService;
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
@RequestMapping("/api/income-plans")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Income Planner", description = "Endpoints for strategizing target amounts, setting up passive/active pipelines, and monitoring progress.")
public class IncomePlanController {

    private final IncomeService incomeService;

    public IncomePlanController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @GetMapping
    @Operation(summary = "Get all strategic income plans for current user")
    public ResponseEntity<List<IncomePlan>> getAllIncomePlans() {
        return ResponseEntity.ok(incomeService.getAllIncomePlansForCurrentUser());
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get a single income plan by ID")
    public ResponseEntity<IncomePlan> getIncomePlanById(@PathVariable UUID planId) {
        return ResponseEntity.ok(incomeService.getIncomePlanById(planId));
    }

    @PostMapping
    @Operation(summary = "Create a new income plan book")
    public ResponseEntity<IncomePlan> createIncomePlan(@Valid @RequestBody IncomePlanRequest request) {
        IncomePlan plan = incomeService.createIncomePlan(request);
        return new ResponseEntity<>(plan, HttpStatus.CREATED);
    }

    @PutMapping("/{planId}")
    @Operation(summary = "Update an existing income plan details")
    public ResponseEntity<IncomePlan> updateIncomePlan(@PathVariable UUID planId, @Valid @RequestBody IncomePlanRequest request) {
        IncomePlan plan = incomeService.updateIncomePlan(planId, request);
        return ResponseEntity.ok(plan);
    }

    @DeleteMapping("/{planId}")
    @Operation(summary = "Delete an entire income plan and its sources")
    public ResponseEntity<Void> deleteIncomePlan(@PathVariable UUID planId) {
        incomeService.deleteIncomePlan(planId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planId}/sources")
    @Operation(summary = "Add an expected income source/pipeline to a plan")
    public ResponseEntity<IncomeSource> addSourceToPlan(@PathVariable UUID planId, @Valid @RequestBody IncomeSourceRequest request) {
        IncomeSource source = incomeService.addSourceToPlan(planId, request);
        return new ResponseEntity<>(source, HttpStatus.CREATED);
    }

    @PutMapping("/{planId}/sources/{sourceId}")
    @Operation(summary = "Update an income source pipeline details and received status")
    public ResponseEntity<IncomeSource> updateIncomeSource(@PathVariable UUID planId, @PathVariable UUID sourceId, @Valid @RequestBody IncomeSourceRequest request) {
        IncomeSource source = incomeService.updateIncomeSource(planId, sourceId, request);
        return ResponseEntity.ok(source);
    }

    @DeleteMapping("/{planId}/sources/{sourceId}")
    @Operation(summary = "Deallocate an income source from a plan")
    public ResponseEntity<Void> deleteIncomeSource(@PathVariable UUID planId, @PathVariable UUID sourceId) {
        incomeService.deleteIncomeSource(planId, sourceId);
        return ResponseEntity.noContent().build();
    }
}
