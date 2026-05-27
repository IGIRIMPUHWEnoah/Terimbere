package com.terimbere.budget.repository;

import com.terimbere.budget.model.BillPayment;
import com.terimbere.budget.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, UUID> {
    List<BillPayment> findByBill(Bill bill);
}
