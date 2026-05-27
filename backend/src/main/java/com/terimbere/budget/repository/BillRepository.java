package com.terimbere.budget.repository;

import com.terimbere.budget.model.Bill;
import com.terimbere.budget.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {
    List<Bill> findByUser(User user);
    List<Bill> findByUserAndStatus(User user, String status);

    @Query("SELECT b FROM Bill b WHERE b.user = :user AND b.dueDate <= :targetDate AND b.status <> 'PAID'")
    List<Bill> findUpcomingUnpaidBills(@Param("user") User user, @Param("targetDate") LocalDate targetDate);
}
