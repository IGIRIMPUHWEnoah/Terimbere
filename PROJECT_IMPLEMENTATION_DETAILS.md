# Terimbere Project - Implementation Details

This document outlines where the required Spring Boot and database concepts have been implemented in the backend project for grading purposes.

## 1. JPQL (Java Persistence Query Language)
**Description:** JPQL is used to query the database using JPA entity objects rather than database tables.
**Location:** `backend/src/main/java/com/terimbere/budget/repository/`
- `DebtRecordRepository.java`: Used heavily to join contacts and apply complex constraints. Example: `@Query("SELECT d FROM DebtRecord d JOIN FETCH d.contact WHERE d.user = :user AND d.dueDate < :today AND d.status <> 'PAID' AND d.status <> 'OVERDUE'")`
- `BudgetRepository.java`: Used to fetch budgets along with their entries. Example: `@Query("SELECT DISTINCT b FROM Budget b LEFT JOIN FETCH b.entries WHERE b.user = :user ORDER BY b.createdAt DESC")`

## 2. Native Query
**Description:** Native queries allow executing raw SQL directly against the underlying database.
**Location:** `backend/src/main/java/com/terimbere/budget/repository/UserRepository.java`
- Implemented `countAllUsersNative()` to quickly count registered users via direct SQL:
  `@Query(value = "SELECT COUNT(*) FROM users", nativeQuery = true)`

## 3. Scheduler (Cron Jobs)
**Description:** Task scheduling runs automated background jobs at specific intervals.
**Location:** `backend/src/main/java/com/terimbere/budget/service/DebtScheduler.java`
- The `markOverdueDebts()` method is annotated with `@Scheduled(cron = "0 0 0 * * ?")`, which automatically runs at midnight every day to check for and update overdue debts.

## 4. Filtering
**Description:** Filtering narrows down query results based on specific criteria.
**Location:** 
- **Repositories (`DebtRecordRepository.java`)**: Filters are applied inside JPQL using `WHERE` clauses (e.g., filtering out 'PAID' statuses or filtering by `dueDate`). Spring Data derived queries also apply filters, like `findByUserAndDebtDirection(User user, String debtDirection, Pageable pageable)`.
- **Services (`DebtService.java`)**: Methods like `getDebtorsOrCreditors(String direction, Pageable pageable)` use parameters to dynamically filter records before returning them to the controller.

## 5. Sorting
**Description:** Sorting orders the data based on specific fields (ascending or descending).
**Location:**
- **Controllers (`DebtController.java`)**: Implemented dynamically via Spring Data's `Sort`. Example: `Sort.by("dueDate").ascending()` ensures that debts closest to their due dates appear first.
- **Repositories (`BudgetRepository.java`)**: Enforced at the query level using JPQL: `ORDER BY b.createdAt DESC`.

## 6. Pagination
**Description:** Pagination limits the number of records retrieved at once, breaking them into smaller "pages" to improve performance.
**Location:**
- **Controllers (`DebtController.java`)**: Creates pagination requests taking page and size arguments: `PageRequest.of(page, size, Sort.by(...))`.
- **Services (`DebtService.java`)**: Methods accept `Pageable pageable` and pass it down.
- **Repositories (`DebtRecordRepository.java`)**: Methods return `Page<DebtRecord>` instead of lists. Example: `Page<DebtRecord> findByUserWithContact(User user, Pageable pageable);`

## 7. Unit Testing
**Description:** Tests ensure individual components function exactly as expected in isolation.
**Location:** `backend/src/test/java/com/terimbere/budget/service/`
- **`DebtSchedulerTest.java`**: Pure unit testing using Mockito to verify that the cron job updates statuses correctly without spinning up the database.
- **`DebtServiceTest.java`**: Integration/Unit testing with `@SpringBootTest` and Mockito (`@MockBean`) to verify the creation and payment logic of debt records.
