package com.terimbere.budget.repository;

import com.terimbere.budget.model.DebtPayment;
import com.terimbere.budget.model.DebtRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DebtPaymentRepository extends JpaRepository<DebtPayment, UUID> {
    List<DebtPayment> findByDebtRecord(DebtRecord debtRecord);
}
