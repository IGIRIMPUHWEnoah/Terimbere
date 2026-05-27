package com.terimbere.budget.controller;

import com.terimbere.budget.dto.request.ContactRequest;
import com.terimbere.budget.dto.request.DebtPaymentRequest;
import com.terimbere.budget.dto.request.DebtRecordRequest;
import com.terimbere.budget.model.Contact;
import com.terimbere.budget.model.DebtPayment;
import com.terimbere.budget.model.DebtRecord;
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

    public DebtController(DebtService debtService) {
        this.debtService = debtService;
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
    @Operation(summary = "Get all debt records for current user")
    public ResponseEntity<List<DebtRecord>> getAllDebtRecords() {
        return ResponseEntity.ok(debtService.getAllDebtRecordsForCurrentUser());
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
}
