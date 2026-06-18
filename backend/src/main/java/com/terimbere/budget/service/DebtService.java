package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.ContactRequest;
import com.terimbere.budget.dto.request.DebtPaymentRequest;
import com.terimbere.budget.dto.request.DebtRecordRequest;
import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.*;
import com.terimbere.budget.repository.ContactRepository;
import com.terimbere.budget.repository.DebtInstallmentRepository;
import com.terimbere.budget.repository.DebtPaymentRepository;
import com.terimbere.budget.repository.DebtRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DebtService {

    private final ContactRepository contactRepository;
    private final DebtRecordRepository debtRecordRepository;
    private final DebtPaymentRepository debtPaymentRepository;
    private final DebtInstallmentRepository debtInstallmentRepository;
    private final AuthService authService;

    public DebtService(ContactRepository contactRepository,
                       DebtRecordRepository debtRecordRepository,
                       DebtPaymentRepository debtPaymentRepository,
                       DebtInstallmentRepository debtInstallmentRepository,
                       AuthService authService) {
        this.contactRepository = contactRepository;
        this.debtRecordRepository = debtRecordRepository;
        this.debtPaymentRepository = debtPaymentRepository;
        this.debtInstallmentRepository = debtInstallmentRepository;
        this.authService = authService;
    }

    // --- CONTACT CRUD ---
    @Transactional(readOnly = true)
    public List<Contact> getAllContactsForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return contactRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Contact getContactById(UUID contactId) {
        User user = authService.getCurrentAuthenticatedUser();
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        if (!contact.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to contact");
        }
        return contact;
    }

    @Transactional
    public Contact createContact(ContactRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        Contact contact = Contact.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .contactType(request.getContactType())
                .notes(request.getNotes())
                .build();
        return contactRepository.save(contact);
    }

    @Transactional
    public Contact updateContact(UUID contactId, ContactRequest request) {
        Contact contact = getContactById(contactId);
        contact.setFullName(request.getFullName());
        contact.setPhone(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setAddress(request.getAddress());
        contact.setContactType(request.getContactType());
        contact.setNotes(request.getNotes());
        return contactRepository.save(contact);
    }

    @Transactional
    public void deleteContact(UUID contactId) {
        Contact contact = getContactById(contactId);
        contactRepository.delete(contact);
    }

    // --- DEBT RECORD CRUD ---
    @Transactional(readOnly = true)
    public Page<DebtRecord> getAllDebtRecordsForCurrentUser(Pageable pageable) {
        User user = authService.getCurrentAuthenticatedUser();
        return debtRecordRepository.findByUserWithContact(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<DebtRecord> getDebtorsOrCreditors(String direction, Pageable pageable) {
        User user = authService.getCurrentAuthenticatedUser();
        return debtRecordRepository.findByUserAndDebtDirection(user, direction, pageable);
    }

    @Transactional(readOnly = true)
    public DebtRecord getDebtRecordById(UUID debtId) {
        User user = authService.getCurrentAuthenticatedUser();
        DebtRecord debt = debtRecordRepository.findById(debtId)
                .orElseThrow(() -> new ResourceNotFoundException("Debt record not found"));
        if (!debt.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to debt record");
        }
        return debt;
    }

    @Transactional
    public DebtRecord createDebtRecord(DebtRecordRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        Contact contact = getContactById(request.getContactId());

        String initialStatus = request.getStatus() != null ? request.getStatus() : "ACTIVE";
        
        String schedulingMode = request.getSchedulingMode() != null ? request.getSchedulingMode() : "SINGLE";
        
        DebtRecord debt = DebtRecord.builder()
                .user(user)
                .contact(contact)
                .debtDirection(request.getDebtDirection())
                .originalAmount(request.getOriginalAmount())
                .remainingAmount(request.getOriginalAmount()) // initially original = remaining
                .dueDate(request.getDueDate())
                .status(initialStatus)
                .schedulingMode(schedulingMode)
                .frequency(request.getFrequency())
                .numberOfInstallments(request.getNumberOfInstallments())
                .build();

        debtRecordRepository.save(debt);
        
        if ("SCHEDULED".equalsIgnoreCase(schedulingMode) && request.getNumberOfInstallments() != null && request.getNumberOfInstallments() > 0) {
            BigDecimal installmentAmount = request.getOriginalAmount().divide(new BigDecimal(request.getNumberOfInstallments()), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal currentTotal = BigDecimal.ZERO;
            
            for (int i = 0; i < request.getNumberOfInstallments(); i++) {
                LocalDate instDueDate = request.getDueDate();
                if ("WEEKLY".equalsIgnoreCase(request.getFrequency())) {
                    instDueDate = instDueDate.plusWeeks(i);
                } else if ("BI_WEEKLY".equalsIgnoreCase(request.getFrequency())) {
                    instDueDate = instDueDate.plusWeeks((long) i * 2);
                } else if ("MONTHLY".equalsIgnoreCase(request.getFrequency())) {
                    instDueDate = instDueDate.plusMonths(i);
                }
                
                BigDecimal amtExpected = installmentAmount;
                // Adjust last installment for rounding differences
                if (i == request.getNumberOfInstallments() - 1) {
                    amtExpected = request.getOriginalAmount().subtract(currentTotal);
                }
                
                DebtInstallment installment = DebtInstallment.builder()
                    .debtRecord(debt)
                    .amountExpected(amtExpected)
                    .amountPaid(BigDecimal.ZERO)
                    .dueDate(instDueDate)
                    .status("PENDING")
                    .build();
                debtInstallmentRepository.save(installment);
                currentTotal = currentTotal.add(amtExpected);
            }
        }

        return debt;
    }

    @Transactional
    public DebtRecord updateDebtRecord(UUID debtId, DebtRecordRequest request) {
        DebtRecord debt = getDebtRecordById(debtId);
        Contact contact = getContactById(request.getContactId());

        // Recalculate remaining amount if original amount changed
        BigDecimal difference = request.getOriginalAmount().subtract(debt.getOriginalAmount());
        debt.setOriginalAmount(request.getOriginalAmount());
        debt.setRemainingAmount(debt.getRemainingAmount().add(difference));
        
        debt.setContact(contact);
        debt.setDebtDirection(request.getDebtDirection());
        debt.setDueDate(request.getDueDate());
        if (request.getSchedulingMode() != null) {
            debt.setSchedulingMode(request.getSchedulingMode());
        }
        if (request.getFrequency() != null) {
            debt.setFrequency(request.getFrequency());
        }
        if (request.getNumberOfInstallments() != null) {
            debt.setNumberOfInstallments(request.getNumberOfInstallments());
        }
        
        if (request.getStatus() != null) {
            debt.setStatus(request.getStatus());
        }

        return debtRecordRepository.save(debt);
    }

    @Transactional
    public void deleteDebtRecord(UUID debtId) {
        DebtRecord debt = getDebtRecordById(debtId);
        debtRecordRepository.delete(debt);
    }

    // --- DEBT PAYMENTS ---
    @Transactional(readOnly = true)
    public List<DebtPayment> getPaymentsForDebt(UUID debtId) {
        DebtRecord debt = getDebtRecordById(debtId);
        return debtPaymentRepository.findByDebtRecord(debt);
    }

    @Transactional
    public DebtPayment recordPayment(UUID debtId, DebtPaymentRequest request) {
        DebtRecord debt = getDebtRecordById(debtId);

        if (debt.getStatus().equalsIgnoreCase("PAID")) {
            throw new IllegalArgumentException("This debt record is already fully paid!");
        }

        if (request.getAmountPaid().compareTo(debt.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining debt balance: " + debt.getRemainingAmount());
        }

        BigDecimal newRemaining = debt.getRemainingAmount().subtract(request.getAmountPaid());
        debt.setRemainingAmount(newRemaining);

        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            debt.setStatus("PAID");
        } else {
            debt.setStatus("PARTIALLY_PAID");
        }

        debtRecordRepository.save(debt);

        // Update installments if any
        if ("SCHEDULED".equalsIgnoreCase(debt.getSchedulingMode())) {
            List<DebtInstallment> pendingInstallments = debtInstallmentRepository.findPendingInstallmentsForDebt(debt);
            BigDecimal paymentToApply = request.getAmountPaid();
            for (DebtInstallment inst : pendingInstallments) {
                if (paymentToApply.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal remainingOnInst = inst.getAmountExpected().subtract(inst.getAmountPaid());
                if (paymentToApply.compareTo(remainingOnInst) >= 0) {
                    inst.setAmountPaid(inst.getAmountExpected());
                    inst.setStatus("PAID");
                    paymentToApply = paymentToApply.subtract(remainingOnInst);
                } else {
                    inst.setAmountPaid(inst.getAmountPaid().add(paymentToApply));
                    paymentToApply = BigDecimal.ZERO;
                }
                debtInstallmentRepository.save(inst);
            }
        }

        DebtPayment payment = DebtPayment.builder()
                .debtRecord(debt)
                .amountPaid(request.getAmountPaid())
                .paymentDate(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        return debtPaymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public BigDecimal getRemainingDebtSum(String direction) {
        User user = authService.getCurrentAuthenticatedUser();
        return debtRecordRepository.sumRemainingAmountByUserAndDirection(user, direction);
    }

    @Transactional(readOnly = true)
    public List<DebtRecord> getOverdueDebts() {
        User user = authService.getCurrentAuthenticatedUser();
        return debtRecordRepository.findOverdueDebts(user, LocalDate.now());
    }
}
