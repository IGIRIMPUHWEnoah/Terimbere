package com.terimbere.budget.controller;

import com.terimbere.budget.dto.request.ContactRequest;
import com.terimbere.budget.dto.request.DebtPaymentRequest;
import com.terimbere.budget.dto.request.DebtRecordRequest;
import com.terimbere.budget.dto.request.SchedulerConfigRequest;
import com.terimbere.budget.dto.response.SchedulerConfigResponse;
import com.terimbere.budget.model.Contact;
import com.terimbere.budget.model.DebtPayment;
import com.terimbere.budget.model.DebtRecord;
import com.terimbere.budget.model.NotificationSettings;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.NotificationSettingsRepository;
import com.terimbere.budget.service.AuthService;
import com.terimbere.budget.service.DebtScheduler;
import com.terimbere.budget.service.DebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/debts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Debts Portal", description = "Endpoints for keeping track of debtors, creditors, partial payment history, and overdue obligations.")
public class DebtController {

    private final DebtService debtService;
    private final AuthService authService;
    private final NotificationSettingsRepository settingsRepository;
    private final DebtScheduler debtScheduler;

    public DebtController(DebtService debtService,
                          AuthService authService,
                          NotificationSettingsRepository settingsRepository,
                          DebtScheduler debtScheduler) {
        this.debtService = debtService;
        this.authService = authService;
        this.settingsRepository = settingsRepository;
        this.debtScheduler = debtScheduler;
    }

    // --- CONTACTS ---
    @GetMapping("/contacts")
    @Operation(summary = "Get all commercial contacts for current user")
    public ResponseEntity<List<Contact>> getAllContacts() {
        return ResponseEntity.ok(debtService.getAllContactsForCurrentUser());
    }

    @GetMapping("/contacts/{contactId}")
    @Operation(summary = "Get a single contact by ID")
    public ResponseEntity<Contact> getContactById(@PathVariable UUID contactId) {
        return ResponseEntity.ok(debtService.getContactById(contactId));
    }

    @PostMapping("/contacts")
    @Operation(summary = "Create a new contact (Debtor, Creditor, or Both)")
    public ResponseEntity<Contact> createContact(@Valid @RequestBody ContactRequest request) {
        Contact contact = debtService.createContact(request);
        return new ResponseEntity<>(contact, HttpStatus.CREATED);
    }

    @PutMapping("/contacts/{contactId}")
    @Operation(summary = "Update an existing contact details")
    public ResponseEntity<Contact> updateContact(@PathVariable UUID contactId, @Valid @RequestBody ContactRequest request) {
        Contact contact = debtService.updateContact(contactId, request);
        return ResponseEntity.ok(contact);
    }

    @DeleteMapping("/contacts/{contactId}")
    @Operation(summary = "Delete an existing contact")
    public ResponseEntity<Void> deleteContact(@PathVariable UUID contactId) {
        debtService.deleteContact(contactId);
        return ResponseEntity.noContent().build();
    }

    // --- DEBT RECORDS ---
    @GetMapping
    @Operation(summary = "Get all debt records for current user with pagination")
    public ResponseEntity<org.springframework.data.domain.Page<DebtRecord>> getAllDebtRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(
                        sort[1].equalsIgnoreCase("desc") ? 
                        org.springframework.data.domain.Sort.Direction.DESC : 
                        org.springframework.data.domain.Sort.Direction.ASC, 
                        sort[0]));
                        
        return ResponseEntity.ok(debtService.getAllDebtRecordsForCurrentUser(pageable));
    }

    @GetMapping("/filter")
    @Operation(summary = "Get debtors or creditors with pagination", description = "Query param direction: 'THEY_OWE_ME' for debtors, or 'I_OWE_THEM' for creditors.")
    public ResponseEntity<org.springframework.data.domain.Page<DebtRecord>> getDebtorsOrCreditors(
            @RequestParam String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
            
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(
                        sort[1].equalsIgnoreCase("desc") ? 
                        org.springframework.data.domain.Sort.Direction.DESC : 
                        org.springframework.data.domain.Sort.Direction.ASC, 
                        sort[0]));
                        
        return ResponseEntity.ok(debtService.getDebtorsOrCreditors(direction, pageable));
    }

    @GetMapping("/{debtId}")
    @Operation(summary = "Get a single debt record by ID")
    public ResponseEntity<DebtRecord> getDebtRecordById(@PathVariable UUID debtId) {
        return ResponseEntity.ok(debtService.getDebtRecordById(debtId));
    }

    @PostMapping
    @Operation(summary = "Record a new debt transaction")
    public ResponseEntity<DebtRecord> createDebtRecord(@Valid @RequestBody DebtRecordRequest request) {
        DebtRecord debt = debtService.createDebtRecord(request);
        return new ResponseEntity<>(debt, HttpStatus.CREATED);
    }

    @PutMapping("/{debtId}")
    @Operation(summary = "Update an existing debt record details")
    public ResponseEntity<DebtRecord> updateDebtRecord(@PathVariable UUID debtId, @Valid @RequestBody DebtRecordRequest request) {
        DebtRecord debt = debtService.updateDebtRecord(debtId, request);
        return ResponseEntity.ok(debt);
    }

    @DeleteMapping("/{debtId}")
    @Operation(summary = "Delete an existing debt record")
    public ResponseEntity<Void> deleteDebtRecord(@PathVariable UUID debtId) {
        debtService.deleteDebtRecord(debtId);
        return ResponseEntity.noContent().build();
    }

    // --- PAYMENTS & SUMMARY STATS ---
    @GetMapping("/{debtId}/payments")
    @Operation(summary = "Get all partial payments logged for a debt record")
    public ResponseEntity<List<DebtPayment>> getPaymentsForDebt(@PathVariable UUID debtId) {
        return ResponseEntity.ok(debtService.getPaymentsForDebt(debtId));
    }

    @PostMapping("/{debtId}/payments")
    @Operation(summary = "Record a partial payment towards a debt record", description = "Deducts the payment amount, updates the remaining balance, and computes new status (PARTIALLY_PAID or PAID).")
    public ResponseEntity<DebtPayment> recordPayment(@PathVariable UUID debtId, @Valid @RequestBody DebtPaymentRequest request) {
        DebtPayment payment = debtService.recordPayment(debtId, request);
        return new ResponseEntity<>(payment, HttpStatus.CREATED);
    }

    @GetMapping("/remaining-sum")
    @Operation(summary = "Get total remaining balance of debts", description = "Query param direction: 'THEY_OWE_ME' for receivables, or 'I_OWE_THEM' for payables.")
    public ResponseEntity<BigDecimal> getRemainingDebtSum(@RequestParam String direction) {
        return ResponseEntity.ok(debtService.getRemainingDebtSum(direction));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get all active debt records where due date has passed")
    public ResponseEntity<List<DebtRecord>> getOverdueDebts() {
        return ResponseEntity.ok(debtService.getOverdueDebts());
    }

    // --- SCHEDULER CONFIGURATION ---
    @GetMapping("/scheduler")
    @Operation(summary = "Get current user's debt check scheduler configuration",
               description = "Returns the hour and minute when the daily overdue-debt check is scheduled to run.")
    public ResponseEntity<SchedulerConfigResponse> getSchedulerConfig() {
        User user = authService.getCurrentAuthenticatedUser();
        NotificationSettings settings = user.getNotificationSettings();
        int hour = (settings != null && settings.getDebtCheckHour() != null) ? settings.getDebtCheckHour() : 0;
        int minute = (settings != null && settings.getDebtCheckMinute() != null) ? settings.getDebtCheckMinute() : 0;

        return ResponseEntity.ok(SchedulerConfigResponse.builder()
                .hour(hour)
                .minute(minute)
                .cronExpression(String.format("0 %d %d * * ?", minute, hour))
                .displayTime(String.format("%02d:%02d", hour, minute))
                .build());
    }

    @PutMapping("/scheduler")
    @Operation(summary = "Update the debt check scheduler time",
               description = "Set the hour (0–23) and minute (0–59) for the daily overdue-debt check. Changes take effect immediately.")
    public ResponseEntity<SchedulerConfigResponse> updateSchedulerConfig(@Valid @RequestBody SchedulerConfigRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        NotificationSettings settings = user.getNotificationSettings();
        if (settings == null) {
            throw new IllegalStateException("Notification settings not found for user.");
        }

        settings.setDebtCheckHour(request.getHour());
        settings.setDebtCheckMinute(request.getMinute());
        settingsRepository.save(settings);

        // Reschedule the task dynamically
        debtScheduler.scheduleForUser(user);

        int hour = request.getHour();
        int minute = request.getMinute();
        return ResponseEntity.ok(SchedulerConfigResponse.builder()
                .hour(hour)
                .minute(minute)
                .cronExpression(String.format("0 %d %d * * ?", minute, hour))
                .displayTime(String.format("%02d:%02d", hour, minute))
                .build());
    }

    @PostMapping("/scheduler/run-now")
    @Operation(summary = "Trigger the overdue-debt check manually right now")
    public ResponseEntity<String> runSchedulerNow() {
        User user = authService.getCurrentAuthenticatedUser();
        debtScheduler.runOverdueCheckForUser(user.getId());
        return ResponseEntity.ok("Overdue debt check executed successfully.");
    }
}
