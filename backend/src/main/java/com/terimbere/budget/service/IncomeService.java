package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.IncomePlanRequest;
import com.terimbere.budget.dto.request.IncomeSourceRequest;
import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.IncomePlan;
import com.terimbere.budget.model.IncomeSource;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.IncomePlanRepository;
import com.terimbere.budget.repository.IncomeSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class IncomeService {

    private final IncomePlanRepository incomePlanRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final AuthService authService;

    public IncomeService(IncomePlanRepository incomePlanRepository,
                         IncomeSourceRepository incomeSourceRepository,
                         AuthService authService) {
        this.incomePlanRepository = incomePlanRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<IncomePlan> getAllIncomePlansForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return incomePlanRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public IncomePlan getIncomePlanById(UUID planId) {
        User user = authService.getCurrentAuthenticatedUser();
        IncomePlan plan = incomePlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Income plan not found"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to income plan");
        }
        return plan;
    }

    @Transactional
    public IncomePlan createIncomePlan(IncomePlanRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        IncomePlan plan = IncomePlan.builder()
                .user(user)
                .title(request.getTitle())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .strategyNotes(request.getStrategyNotes())
                .status(request.getStatus())
                .build();
        return incomePlanRepository.save(plan);
    }

    @Transactional
    public IncomePlan updateIncomePlan(UUID planId, IncomePlanRequest request) {
        IncomePlan plan = getIncomePlanById(planId);
        plan.setTitle(request.getTitle());
        plan.setTargetAmount(request.getTargetAmount());
        plan.setTargetDate(request.getTargetDate());
        plan.setStrategyNotes(request.getStrategyNotes());
        plan.setStatus(request.getStatus());
        return incomePlanRepository.save(plan);
    }

    @Transactional
    public void deleteIncomePlan(UUID planId) {
        IncomePlan plan = getIncomePlanById(planId);
        incomePlanRepository.delete(plan);
    }

    // --- INCOME SOURCES / PIPELINES ---
    @Transactional
    public IncomeSource addSourceToPlan(UUID planId, IncomeSourceRequest request) {
        IncomePlan plan = getIncomePlanById(planId);

        BigDecimal received = request.getReceivedAmount() != null ? request.getReceivedAmount() : BigDecimal.ZERO;
        String status = "PENDING";
        if (received.compareTo(BigDecimal.ZERO) > 0) {
            if (received.compareTo(request.getExpectedAmount()) >= 0) {
                status = "RECEIVED";
            } else {
                status = "PARTIAL";
            }
        }

        IncomeSource source = IncomeSource.builder()
                .sourceName(request.getSourceName())
                .expectedAmount(request.getExpectedAmount())
                .receivedAmount(received)
                .status(status)
                .build();

        plan.addSource(source);
        incomePlanRepository.save(plan);
        recalculatePlanStatus(plan);
        return source;
    }

    @Transactional
    public IncomeSource updateIncomeSource(UUID planId, UUID sourceId, IncomeSourceRequest request) {
        IncomePlan plan = getIncomePlanById(planId);
        IncomeSource source = incomeSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Income source not found"));

        if (!source.getIncomePlan().getId().equals(plan.getId())) {
            throw new IllegalArgumentException("Source does not belong to this plan");
        }

        source.setSourceName(request.getSourceName());
        source.setExpectedAmount(request.getExpectedAmount());
        if (request.getReceivedAmount() != null) {
            source.setReceivedAmount(request.getReceivedAmount());
        }

        // recalculate status
        if (source.getReceivedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            source.setStatus("PENDING");
        } else if (source.getReceivedAmount().compareTo(source.getExpectedAmount()) >= 0) {
            source.setStatus("RECEIVED");
        } else {
            source.setStatus("PARTIAL");
        }

        IncomeSource updated = incomeSourceRepository.save(source);
        recalculatePlanStatus(plan);
        return updated;
    }

    @Transactional
    public void deleteIncomeSource(UUID planId, UUID sourceId) {
        IncomePlan plan = getIncomePlanById(planId);
        IncomeSource source = incomeSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Income source not found"));

        if (!source.getIncomePlan().getId().equals(plan.getId())) {
            throw new IllegalArgumentException("Source does not belong to this plan");
        }

        plan.removeSource(source);
        incomePlanRepository.save(plan);
        recalculatePlanStatus(plan);
    }

    private void recalculatePlanStatus(IncomePlan plan) {
        if (plan.getSources().isEmpty()) {
            return;
        }

        boolean allReceived = true;
        boolean anyReceivedOrPartial = false;

        for (IncomeSource source : plan.getSources()) {
            if (!source.getStatus().equals("RECEIVED")) {
                allReceived = false;
            }
            if (source.getStatus().equals("RECEIVED") || source.getStatus().equals("PARTIAL")) {
                anyReceivedOrPartial = true;
            }
        }

        if (allReceived) {
            plan.setStatus("ACHIEVED");
        } else if (anyReceivedOrPartial) {
            plan.setStatus("ACTIVE");
        } else {
            plan.setStatus("DRAFT");
        }

        incomePlanRepository.save(plan);
    }
}
