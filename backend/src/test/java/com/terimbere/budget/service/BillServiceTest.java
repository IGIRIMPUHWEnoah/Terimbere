package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.BillPaymentRequest;
import com.terimbere.budget.dto.request.BillRequest;
import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.Bill;
import com.terimbere.budget.model.BillPayment;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.BillRepository;
import com.terimbere.budget.repository.UserRepository;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BillServiceTest {

    @Autowired
    private BillService billService;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("bill-test@terimbere.com")
                .passwordHash("hashed")
                .fullName("Bill Tester")
                .build();
        testUser = userRepository.save(testUser);

        Mockito.when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Test
    void createBill_validRequest_persistsAndReturnsCorrectData() {
        BillRequest req = buildBillRequest("Internet", "UTILITIES", "50000", false, "ONCE", "UNPAID");

        Bill saved = billService.createBill(req);

        assertNotNull(saved.getId());
        assertEquals("Internet", saved.getTitle());
        assertEquals("UTILITIES", saved.getCategory());
        assertEquals(new BigDecimal("50000"), saved.getAmount());
        assertEquals("UNPAID", saved.getStatus());
        assertFalse(saved.getIsRecurring());
    }

    @Test
    void createBill_nullIsRecurring_defaultsToFalse() {
        BillRequest req = buildBillRequest("Water Bill", "UTILITIES", "3000", null, "ONCE", "UNPAID");

        Bill saved = billService.createBill(req);

        assertFalse(saved.getIsRecurring());
    }

    @Test
    void createBill_recurringBill_flagIsSet() {
        BillRequest req = buildBillRequest("Netflix", "ENTERTAINMENT", "12000", true, "MONTHLY", "UNPAID");

        Bill saved = billService.createBill(req);

        assertTrue(saved.getIsRecurring());
        assertEquals("MONTHLY", saved.getRecurrencePeriod());
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Test
    void getAllBillsForCurrentUser_returnsOnlyUserBills() {
        // Create 2 bills for testUser and 1 for another user
        billService.createBill(buildBillRequest("Bill A", "FOOD", "1000", false, "ONCE", "UNPAID"));
        billService.createBill(buildBillRequest("Bill B", "FOOD", "2000", false, "ONCE", "UNPAID"));

        User otherUser = userRepository.save(User.builder()
                .email("other@terimbere.com")
                .passwordHash("hashed")
                .fullName("Other User")
                .build());
        Bill foreignBill = Bill.builder()
                .user(otherUser)
                .title("Foreign Bill")
                .category("MISC")
                .amount(new BigDecimal("9999"))
                .dueDate(LocalDate.now().plusDays(10))
                .isRecurring(false)
                .recurrencePeriod("ONCE")
                .status("UNPAID")
                .build();
        billRepository.save(foreignBill);

        List<Bill> result = billService.getAllBillsForCurrentUser();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(b -> b.getUser().getId().equals(testUser.getId())));
    }

    @Test
    void getBillById_existingId_returnsBill() {
        Bill saved = billService.createBill(buildBillRequest("Rent", "HOUSING", "200000", false, "MONTHLY", "UNPAID"));

        Bill found = billService.getBillById(saved.getId());

        assertEquals(saved.getId(), found.getId());
        assertEquals("Rent", found.getTitle());
    }

    @Test
    void getBillById_nonExistentId_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class,
                () -> billService.getBillById(UUID.randomUUID()));
    }

    @Test
    void getBillById_otherUsersBill_throwsSecurityException() {
        User otherUser = userRepository.save(User.builder()
                .email("intruder@terimbere.com")
                .passwordHash("hashed")
                .fullName("Intruder")
                .build());
        Bill otherBill = billRepository.save(Bill.builder()
                .user(otherUser)
                .title("Locked Bill")
                .category("MISC")
                .amount(new BigDecimal("5000"))
                .dueDate(LocalDate.now().plusDays(5))
                .isRecurring(false)
                .recurrencePeriod("ONCE")
                .status("UNPAID")
                .build());

        assertThrows(SecurityException.class,
                () -> billService.getBillById(otherBill.getId()));
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Test
    void updateBill_changesFields_persistsCorrectly() {
        Bill original = billService.createBill(buildBillRequest("Old Title", "UTILITIES", "1000", false, "ONCE", "UNPAID"));

        BillRequest update = buildBillRequest("New Title", "TRANSPORT", "2500", true, "WEEKLY", "UNPAID");
        Bill updated = billService.updateBill(original.getId(), update);

        assertEquals("New Title", updated.getTitle());
        assertEquals("TRANSPORT", updated.getCategory());
        assertEquals(new BigDecimal("2500"), updated.getAmount());
        assertTrue(updated.getIsRecurring());
        assertEquals("WEEKLY", updated.getRecurrencePeriod());
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Test
    void deleteBill_existingBill_removedFromDatabase() {
        Bill saved = billService.createBill(buildBillRequest("Temp Bill", "MISC", "500", false, "ONCE", "UNPAID"));
        UUID id = saved.getId();

        billService.deleteBill(id);

        assertFalse(billRepository.findById(id).isPresent());
    }

    @Test
    void deleteBill_nonExistentId_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class,
                () -> billService.deleteBill(UUID.randomUUID()));
    }

    // -------------------------------------------------------------------------
    // Pay Bill
    // -------------------------------------------------------------------------

    @Test
    void payBill_nonRecurring_statusBecomePaid() {
        Bill bill = billService.createBill(buildBillRequest("One-off Bill", "FOOD", "8000", false, "ONCE", "UNPAID"));

        BillPaymentRequest payReq = new BillPaymentRequest();
        payReq.setAmountPaid(new BigDecimal("8000"));
        payReq.setPaymentMethod("Mobile Money");
        payReq.setTransactionRef("TXN-001");

        BillPayment payment = billService.payBill(bill.getId(), payReq);

        assertNotNull(payment.getId());
        assertEquals(new BigDecimal("8000"), payment.getAmountPaid());

        Bill paidBill = billService.getBillById(bill.getId());
        assertEquals("PAID", paidBill.getStatus());
    }

    @Test
    void payBill_monthlyRecurring_rollsDueDateForwardAndStaysUnpaid() {
        LocalDate originalDue = LocalDate.now().plusDays(5);
        BillRequest req = buildBillRequest("Monthly Sub", "ENTERTAINMENT", "5000", true, "MONTHLY", "UNPAID");
        req.setDueDate(originalDue);
        Bill bill = billService.createBill(req);

        BillPaymentRequest payReq = new BillPaymentRequest();
        payReq.setAmountPaid(new BigDecimal("5000"));
        payReq.setPaymentMethod("Card");

        billService.payBill(bill.getId(), payReq);

        Bill updated = billService.getBillById(bill.getId());
        assertEquals("UNPAID", updated.getStatus());
        assertEquals(originalDue.plusMonths(1), updated.getDueDate());
    }

    @Test
    void payBill_weeklyRecurring_rollsDueDateByOneWeek() {
        LocalDate originalDue = LocalDate.now().plusDays(3);
        BillRequest req = buildBillRequest("Weekly Service", "MISC", "1000", true, "WEEKLY", "UNPAID");
        req.setDueDate(originalDue);
        Bill bill = billService.createBill(req);

        BillPaymentRequest payReq = new BillPaymentRequest();
        payReq.setAmountPaid(new BigDecimal("1000"));

        billService.payBill(bill.getId(), payReq);

        assertEquals(originalDue.plusWeeks(1), billService.getBillById(bill.getId()).getDueDate());
    }

    @Test
    void payBill_yearlyRecurring_rollsDueDateByOneYear() {
        LocalDate originalDue = LocalDate.now().plusDays(1);
        BillRequest req = buildBillRequest("Annual License", "SOFTWARE", "60000", true, "YEARLY", "UNPAID");
        req.setDueDate(originalDue);
        Bill bill = billService.createBill(req);

        BillPaymentRequest payReq = new BillPaymentRequest();
        payReq.setAmountPaid(new BigDecimal("60000"));

        billService.payBill(bill.getId(), payReq);

        assertEquals(originalDue.plusYears(1), billService.getBillById(bill.getId()).getDueDate());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private BillRequest buildBillRequest(String title, String category, String amount,
                                          Boolean isRecurring, String period, String status) {
        BillRequest req = new BillRequest();
        req.setTitle(title);
        req.setCategory(category);
        req.setAmount(new BigDecimal(amount));
        req.setDueDate(LocalDate.now().plusDays(7));
        req.setIsRecurring(isRecurring);
        req.setRecurrencePeriod(period);
        req.setStatus(status);
        return req;
    }
}
