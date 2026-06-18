package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.BudgetEntryRequest;
import com.terimbere.budget.dto.request.BudgetRequest;
import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.Budget;
import com.terimbere.budget.model.BudgetEntry;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.BudgetRepository;
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
public class BudgetServiceTest {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("budget-test@terimbere.com")
                .passwordHash("hashed")
                .fullName("Budget Tester")
                .build();
        testUser = userRepository.save(testUser);

        Mockito.when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
    }

    // -------------------------------------------------------------------------
    // Create Budget
    // -------------------------------------------------------------------------

    @Test
    void createBudget_validRequest_persistsAndReturnsCorrectData() {
        BudgetRequest req = buildBudgetRequest("Monthly Budget", "MONTHLY", "ACTIVE");

        Budget saved = budgetService.createBudget(req);

        assertNotNull(saved.getId());
        assertEquals("Monthly Budget", saved.getName());
        assertEquals("MONTHLY", saved.getPeriodType());
        assertEquals("ACTIVE", saved.getStatus());
    }

    @Test
    void createBudget_nullBudgetType_defaultsToPersonal() {
        BudgetRequest req = buildBudgetRequest("Personal Budget", "YEARLY", "DRAFT");
        req.setBudgetType(null);

        Budget saved = budgetService.createBudget(req);

        assertEquals("PERSONAL", saved.getBudgetType());
    }

    @Test
    void createBudget_withCustomBudgetType_savedCorrectly() {
        BudgetRequest req = buildBudgetRequest("Project Budget", "CUSTOM", "ACTIVE");
        req.setBudgetType("PROJECT");
        req.setProjectTotalBudget(new BigDecimal("500000"));

        Budget saved = budgetService.createBudget(req);

        assertEquals("PROJECT", saved.getBudgetType());
        assertEquals(new BigDecimal("500000"), saved.getProjectTotalBudget());
    }

    @Test
    void createBudget_withSavingsGoal_savedCorrectly() {
        BudgetRequest req = buildBudgetRequest("Savings Plan", "YEARLY", "ACTIVE");
        req.setBudgetType("SAVINGS");
        req.setSavingsGoal(new BigDecimal("1000000"));

        Budget saved = budgetService.createBudget(req);

        assertEquals(new BigDecimal("1000000"), saved.getSavingsGoal());
    }

    // -------------------------------------------------------------------------
    // Read Budget
    // -------------------------------------------------------------------------

    @Test
    void getAllBudgetsForCurrentUser_returnsOnlyUserBudgets() {
        budgetService.createBudget(buildBudgetRequest("Budget A", "MONTHLY", "ACTIVE"));
        budgetService.createBudget(buildBudgetRequest("Budget B", "QUARTERLY", "DRAFT"));

        User otherUser = userRepository.save(User.builder()
                .email("other-budget@terimbere.com")
                .passwordHash("hashed")
                .fullName("Other User")
                .build());
        budgetRepository.save(Budget.builder()
                .user(otherUser)
                .name("Other Budget")
                .periodType("MONTHLY")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status("ACTIVE")
                .budgetType("PERSONAL")
                .build());

        List<Budget> result = budgetService.getAllBudgetsForCurrentUser();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(b -> b.getUser().getId().equals(testUser.getId())));
    }

    @Test
    void getBudgetById_existingId_returnsBudget() {
        Budget saved = budgetService.createBudget(buildBudgetRequest("Find Me", "MONTHLY", "ACTIVE"));

        Budget found = budgetService.getBudgetById(saved.getId());

        assertEquals(saved.getId(), found.getId());
        assertEquals("Find Me", found.getName());
    }

    @Test
    void getBudgetById_nonExistentId_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class,
                () -> budgetService.getBudgetById(UUID.randomUUID()));
    }

    @Test
    void getBudgetById_otherUsersBudget_throwsSecurityException() {
        User otherUser = userRepository.save(User.builder()
                .email("thief@terimbere.com")
                .passwordHash("hashed")
                .fullName("Thief User")
                .build());
        Budget otherBudget = budgetRepository.save(Budget.builder()
                .user(otherUser)
                .name("Private Budget")
                .periodType("MONTHLY")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status("ACTIVE")
                .budgetType("PERSONAL")
                .build());

        assertThrows(SecurityException.class,
                () -> budgetService.getBudgetById(otherBudget.getId()));
    }

    // -------------------------------------------------------------------------
    // Update Budget
    // -------------------------------------------------------------------------

    @Test
    void updateBudget_changesFields_persistsCorrectly() {
        Budget original = budgetService.createBudget(buildBudgetRequest("Old Name", "MONTHLY", "DRAFT"));

        BudgetRequest update = buildBudgetRequest("New Name", "QUARTERLY", "ACTIVE");
        update.setDescription("Updated description");
        Budget updated = budgetService.updateBudget(original.getId(), update);

        assertEquals("New Name", updated.getName());
        assertEquals("QUARTERLY", updated.getPeriodType());
        assertEquals("ACTIVE", updated.getStatus());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    void updateBudget_nullBudgetType_doesNotOverrideExisting() {
        BudgetRequest createReq = buildBudgetRequest("My Budget", "MONTHLY", "ACTIVE");
        createReq.setBudgetType("BUSINESS");
        Budget original = budgetService.createBudget(createReq);

        BudgetRequest update = buildBudgetRequest("My Budget", "MONTHLY", "ACTIVE");
        update.setBudgetType(null); // null should not overwrite existing value
        Budget updated = budgetService.updateBudget(original.getId(), update);

        assertEquals("BUSINESS", updated.getBudgetType());
    }

    // -------------------------------------------------------------------------
    // Delete Budget
    // -------------------------------------------------------------------------

    @Test
    void deleteBudget_existingBudget_removedFromDatabase() {
        Budget saved = budgetService.createBudget(buildBudgetRequest("Temp Budget", "MONTHLY", "DRAFT"));
        UUID id = saved.getId();

        budgetService.deleteBudget(id);

        assertFalse(budgetRepository.findById(id).isPresent());
    }

    @Test
    void deleteBudget_nonExistentId_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class,
                () -> budgetService.deleteBudget(UUID.randomUUID()));
    }

    // -------------------------------------------------------------------------
    // Budget Entries
    // -------------------------------------------------------------------------

    @Test
    void addEntryToBudget_validEntry_persistsAndLinkedToBudget() {
        Budget budget = budgetService.createBudget(buildBudgetRequest("Entry Test Budget", "MONTHLY", "ACTIVE"));

        BudgetEntryRequest entryReq = buildEntryRequest("EXPENSE", "Food", "30000", null);

        BudgetEntry entry = budgetService.addEntryToBudget(budget.getId(), entryReq);

        assertNotNull(entry.getId());
        assertEquals("EXPENSE", entry.getEntryType());
        assertEquals("Food", entry.getCategory());
        assertEquals(new BigDecimal("30000"), entry.getPlannedAmount());
        assertEquals(BigDecimal.ZERO, entry.getActualAmount());
    }

    @Test
    void addEntryToBudget_withActualAmount_savedCorrectly() {
        Budget budget = budgetService.createBudget(buildBudgetRequest("Actual Test Budget", "MONTHLY", "ACTIVE"));

        BudgetEntryRequest entryReq = buildEntryRequest("INCOME", "Salary", "500000", new BigDecimal("480000"));

        BudgetEntry entry = budgetService.addEntryToBudget(budget.getId(), entryReq);

        assertEquals(new BigDecimal("480000"), entry.getActualAmount());
    }

    @Test
    void addMultipleEntriesToBudget_allAreSaved() {
        Budget budget = budgetService.createBudget(buildBudgetRequest("Multi Entry Budget", "MONTHLY", "ACTIVE"));

        budgetService.addEntryToBudget(budget.getId(), buildEntryRequest("INCOME", "Salary", "400000", null));
        budgetService.addEntryToBudget(budget.getId(), buildEntryRequest("EXPENSE", "Rent", "150000", null));
        budgetService.addEntryToBudget(budget.getId(), buildEntryRequest("EXPENSE", "Groceries", "50000", null));

        Budget loaded = budgetService.getBudgetById(budget.getId());
        assertEquals(3, loaded.getEntries().size());
    }

    @Test
    void updateBudgetEntry_changesFields_persistsCorrectly() {
        Budget budget = budgetService.createBudget(buildBudgetRequest("Update Entry Budget", "MONTHLY", "ACTIVE"));
        BudgetEntry entry = budgetService.addEntryToBudget(budget.getId(),
                buildEntryRequest("EXPENSE", "Old Category", "20000", null));

        BudgetEntryRequest update = buildEntryRequest("INCOME", "New Category", "35000", new BigDecimal("35000"));
        BudgetEntry updated = budgetService.updateBudgetEntry(budget.getId(), entry.getId(), update);

        assertEquals("INCOME", updated.getEntryType());
        assertEquals("New Category", updated.getCategory());
        assertEquals(new BigDecimal("35000"), updated.getPlannedAmount());
        assertEquals(new BigDecimal("35000"), updated.getActualAmount());
    }

    @Test
    void removeEntryFromBudget_entryIsDeleted() {
        Budget budget = budgetService.createBudget(buildBudgetRequest("Remove Entry Budget", "MONTHLY", "ACTIVE"));
        BudgetEntry entry = budgetService.addEntryToBudget(budget.getId(),
                buildEntryRequest("EXPENSE", "Temp Expense", "10000", null));

        budgetService.removeEntryFromBudget(budget.getId(), entry.getId());

        Budget loaded = budgetService.getBudgetById(budget.getId());
        assertTrue(loaded.getEntries().isEmpty());
    }

    @Test
    void updateBudgetEntry_entryNotInBudget_throwsIllegalArgumentException() {
        Budget budget1 = budgetService.createBudget(buildBudgetRequest("Budget 1", "MONTHLY", "ACTIVE"));
        Budget budget2 = budgetService.createBudget(buildBudgetRequest("Budget 2", "MONTHLY", "ACTIVE"));

        BudgetEntry entry = budgetService.addEntryToBudget(budget1.getId(),
                buildEntryRequest("EXPENSE", "Food", "5000", null));

        // Try to update an entry under the wrong budget
        assertThrows(IllegalArgumentException.class, () ->
                budgetService.updateBudgetEntry(budget2.getId(), entry.getId(),
                        buildEntryRequest("EXPENSE", "Food", "5000", null)));
    }

    @Test
    void removeEntryFromBudget_entryNotInBudget_throwsIllegalArgumentException() {
        Budget budget1 = budgetService.createBudget(buildBudgetRequest("Budget A", "MONTHLY", "ACTIVE"));
        Budget budget2 = budgetService.createBudget(buildBudgetRequest("Budget B", "MONTHLY", "ACTIVE"));

        BudgetEntry entry = budgetService.addEntryToBudget(budget1.getId(),
                buildEntryRequest("INCOME", "Salary", "100000", null));

        // Try to remove an entry using the wrong budget id
        assertThrows(IllegalArgumentException.class, () ->
                budgetService.removeEntryFromBudget(budget2.getId(), entry.getId()));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BudgetRequest buildBudgetRequest(String name, String periodType, String status) {
        BudgetRequest req = new BudgetRequest();
        req.setName(name);
        req.setPeriodType(periodType);
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusMonths(1));
        req.setStatus(status);
        req.setBudgetType("PERSONAL");
        return req;
    }

    private BudgetEntryRequest buildEntryRequest(String type, String category,
                                                  String planned, BigDecimal actual) {
        BudgetEntryRequest req = new BudgetEntryRequest();
        req.setEntryType(type);
        req.setCategory(category);
        req.setPlannedAmount(new BigDecimal(planned));
        req.setActualAmount(actual);
        req.setEntryDate(LocalDate.now());
        return req;
    }
}
