package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.ContactRequest;
import com.terimbere.budget.dto.request.DebtPaymentRequest;
import com.terimbere.budget.dto.request.DebtRecordRequest;
import com.terimbere.budget.model.*;
import com.terimbere.budget.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DebtServiceTest {

    @Autowired
    private DebtService debtService;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private DebtRecordRepository debtRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create and save test user
        testUser = User.builder()
                .email("test@terimbere.com")
                .passwordHash("password")
                .fullName("Test User")
                .build();
        testUser = userRepository.save(testUser);

        // Mock security context user
        Mockito.when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
    }

    @Test
    void testCreateContactAndDebtRecord() {
        // Create Contact
        ContactRequest contactReq = new ContactRequest();
        contactReq.setFullName("Jean-Luc");
        contactReq.setContactType("DEBTOR");
        contactReq.setPhone("+250788888888");

        Contact contact = debtService.createContact(contactReq);
        assertNotNull(contact.getId());
        assertEquals("Jean-Luc", contact.getFullName());

        // Create Debt Record (Jean-Luc owes testUser 50,000 RWF)
        DebtRecordRequest debtReq = new DebtRecordRequest();
        debtReq.setContactId(contact.getId());
        debtReq.setDebtDirection("THEY_OWE_ME");
        debtReq.setOriginalAmount(new BigDecimal("50000.00"));
        debtReq.setDueDate(LocalDate.now().plusMonths(1));

        DebtRecord debt = debtService.createDebtRecord(debtReq);
        assertNotNull(debt.getId());
        assertEquals(new BigDecimal("50000.00"), debt.getOriginalAmount());
        assertEquals(new BigDecimal("50000.00"), debt.getRemainingAmount());
        assertEquals("ACTIVE", debt.getStatus());
    }

    @Test
    void testRecordPartialAndFullDebtPayment() {
        // Setup contact and debt
        Contact contact = Contact.builder()
                .user(testUser)
                .fullName("Marie")
                .contactType("DEBTOR")
                .build();
        contact = contactRepository.save(contact);

        DebtRecord debt = DebtRecord.builder()
                .user(testUser)
                .contact(contact)
                .debtDirection("THEY_OWE_ME")
                .originalAmount(new BigDecimal("10000.00"))
                .remainingAmount(new BigDecimal("10000.00"))
                .dueDate(LocalDate.now().plusWeeks(2))
                .status("ACTIVE")
                .build();
        debt = debtRecordRepository.save(debt);

        // Record Partial Payment of 4,000 RWF
        DebtPaymentRequest partialPayment = new DebtPaymentRequest();
        partialPayment.setAmountPaid(new BigDecimal("4000.00"));
        partialPayment.setPaymentMethod("Mobile Money");
        partialPayment.setNotes("First installment");

        DebtPayment p1 = debtService.recordPayment(debt.getId(), partialPayment);
        assertNotNull(p1.getId());
        
        DebtRecord updatedDebt = debtService.getDebtRecordById(debt.getId());
        assertEquals(new BigDecimal("6000.00"), updatedDebt.getRemainingAmount());
        assertEquals("PARTIALLY_PAID", updatedDebt.getStatus());

        // Record Full Payment of remaining 6,000 RWF
        DebtPaymentRequest fullPayment = new DebtPaymentRequest();
        fullPayment.setAmountPaid(new BigDecimal("6000.00"));
        fullPayment.setPaymentMethod("Cash");

        DebtPayment p2 = debtService.recordPayment(debt.getId(), fullPayment);
        assertNotNull(p2.getId());

        DebtRecord paidDebt = debtService.getDebtRecordById(debt.getId());
        assertEquals(new BigDecimal("0.00"), paidDebt.getRemainingAmount());
        assertEquals("PAID", paidDebt.getStatus());
    }
}
