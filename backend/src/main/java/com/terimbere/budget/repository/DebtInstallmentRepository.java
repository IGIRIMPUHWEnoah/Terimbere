package com.terimbere.budget.repository;

import com.terimbere.budget.model.DebtInstallment;
import com.terimbere.budget.model.DebtRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DebtInstallmentRepository extends JpaRepository<DebtInstallment, UUID> {
    
    List<DebtInstallment> findByDebtRecordOrderByDueDateAsc(DebtRecord debtRecord);

    @Query("SELECT d FROM DebtInstallment d WHERE d.debtRecord = :debtRecord AND d.status != 'PAID' ORDER BY d.dueDate ASC")
    List<DebtInstallment> findPendingInstallmentsForDebt(@Param("debtRecord") DebtRecord debtRecord);

    @Query("SELECT d FROM DebtInstallment d WHERE d.status = 'PENDING' AND d.dueDate < :today")
    List<DebtInstallment> findOverdueInstallments(@Param("today") LocalDate today);
}
