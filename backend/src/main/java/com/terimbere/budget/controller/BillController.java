package com.terimbere.budget.controller;

import com.terimbere.budget.dto.request.BillPaymentRequest;
import com.terimbere.budget.dto.request.BillRequest;
import com.terimbere.budget.model.Bill;
import com.terimbere.budget.model.BillPayment;
import com.terimbere.budget.service.BillService;
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
@RequestMapping("/api/bills")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bills Manager", description = "Endpoints for scheduling utilities, subscriptions, and recording payment outflows.")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping
    @Operation(summary = "Get all bills for current user")
    public ResponseEntity<List<Bill>> getAllBills() {
        return ResponseEntity.ok(billService.getAllBillsForCurrentUser());
    }

    @GetMapping("/{billId}")
    @Operation(summary = "Get single bill by ID")
    public ResponseEntity<Bill> getBillById(@PathVariable UUID billId) {
        return ResponseEntity.ok(billService.getBillById(billId));
    }

    @PostMapping
    @Operation(summary = "Schedule a new bill or subscription")
    public ResponseEntity<Bill> createBill(@Valid @RequestBody BillRequest request) {
        Bill bill = billService.createBill(request);
        return new ResponseEntity<>(bill, HttpStatus.CREATED);
    }

    @PutMapping("/{billId}")
    @Operation(summary = "Update an existing scheduled bill details")
    public ResponseEntity<Bill> updateBill(@PathVariable UUID billId, @Valid @RequestBody BillRequest request) {
        Bill bill = billService.updateBill(billId, request);
        return ResponseEntity.ok(bill);
    }

    @DeleteMapping("/{billId}")
    @Operation(summary = "Delete an existing scheduled bill")
    public ResponseEntity<Void> deleteBill(@PathVariable UUID billId) {
        billService.deleteBill(billId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{billId}/pay")
    @Operation(summary = "Pay down a bill obligation", description = "Registers payment transaction. If recurring, rolls the due date forward to the next cycle.")
    public ResponseEntity<BillPayment> payBill(@PathVariable UUID billId, @Valid @RequestBody BillPaymentRequest request) {
        BillPayment payment = billService.payBill(billId, request);
        return new ResponseEntity<>(payment, HttpStatus.CREATED);
    }
}
