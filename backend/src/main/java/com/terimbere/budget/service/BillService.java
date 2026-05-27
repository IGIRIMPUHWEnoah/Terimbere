package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.BillPaymentRequest;
import com.terimbere.budget.dto.request.BillRequest;
import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.Bill;
import com.terimbere.budget.model.BillPayment;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.BillPaymentRepository;
import com.terimbere.budget.repository.BillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BillService {

    private final BillRepository billRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final AuthService authService;

    public BillService(BillRepository billRepository,
                       BillPaymentRepository billPaymentRepository,
                       AuthService authService) {
        this.billRepository = billRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<Bill> getAllBillsForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return billRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Bill getBillById(UUID billId) {
        User user = authService.getCurrentAuthenticatedUser();
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        if (!bill.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to bill");
        }
        return bill;
    }

    @Transactional
    public Bill createBill(BillRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        Bill bill = Bill.builder()
                .user(user)
                .title(request.getTitle())
                .category(request.getCategory())
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .recurrencePeriod(request.getRecurrencePeriod())
                .status(request.getStatus())
                .notes(request.getNotes())
                .build();
        return billRepository.save(bill);
    }

    @Transactional
    public Bill updateBill(UUID billId, BillRequest request) {
        Bill bill = getBillById(billId);
        bill.setTitle(request.getTitle());
        bill.setCategory(request.getCategory());
        bill.setAmount(request.getAmount());
        bill.setDueDate(request.getDueDate());
        if (request.getIsRecurring() != null) {
            bill.setIsRecurring(request.getIsRecurring());
        }
        bill.setRecurrencePeriod(request.getRecurrencePeriod());
        bill.setStatus(request.getStatus());
        bill.setNotes(request.getNotes());
        return billRepository.save(bill);
    }

    @Transactional
    public void deleteBill(UUID billId) {
        Bill bill = getBillById(billId);
        billRepository.delete(bill);
    }

    @Transactional
    public BillPayment payBill(UUID billId, BillPaymentRequest request) {
        Bill bill = getBillById(billId);

        // Record the payment
        BillPayment payment = BillPayment.builder()
                .bill(bill)
                .amountPaid(request.getAmountPaid())
                .paymentDate(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .transactionRef(request.getTransactionRef())
                .build();

        billPaymentRepository.save(payment);

        // Check if the bill is recurring
        if (bill.getIsRecurring()) {
            // Roll the due date forward to the next cycle and keep UNPAID
            LocalDate nextDueDate = calculateNextDueDate(bill.getDueDate(), bill.getRecurrencePeriod());
            bill.setDueDate(nextDueDate);
            bill.setStatus("UNPAID");
        } else {
            // Set status to PAID
            bill.setStatus("PAID");
        }

        billRepository.save(bill);
        return payment;
    }

    private LocalDate calculateNextDueDate(LocalDate currentDueDate, String period) {
        switch (period.toUpperCase()) {
            case "WEEKLY":
                return currentDueDate.plusWeeks(1);
            case "MONTHLY":
                return currentDueDate.plusMonths(1);
            case "YEARLY":
                return currentDueDate.plusYears(1);
            default:
                return currentDueDate;
        }
    }
}
