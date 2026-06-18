package com.terimbere.budget.repository;

import com.terimbere.budget.model.DebtRecord;
import com.terimbere.budget.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DebtRecordRepository extends JpaRepository<DebtRecord, UUID> {
    List<DebtRecord> findByUser(User user);
    List<DebtRecord> findByUserAndStatus(User user, String status);

    @EntityGraph(attributePaths = {"contact"})
    @Query("SELECT d FROM DebtRecord d WHERE d.user = :user")
    Page<DebtRecord> findByUserWithContact(@Param("user") User user, Pageable pageable);

    @EntityGraph(attributePaths = {"contact"})
    Page<DebtRecord> findByUserAndDebtDirection(User user, String debtDirection, Pageable pageable);

    @Query("SELECT d FROM DebtRecord d JOIN FETCH d.contact WHERE d.id = :id")
    Optional<DebtRecord> findByIdWithContact(@Param("id") UUID id);

    @Query("SELECT d FROM DebtRecord d JOIN FETCH d.contact WHERE d.user = :user AND d.dueDate < :today AND d.status <> 'PAID' AND d.status <> 'OVERDUE'")
    List<DebtRecord> findOverdueDebts(@Param("user") User user, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(d.remainingAmount), 0) FROM DebtRecord d WHERE d.user = :user AND d.debtDirection = :direction AND d.status <> 'PAID'")
    BigDecimal sumRemainingAmountByUserAndDirection(@Param("user") User user, @Param("direction") String direction);
}
