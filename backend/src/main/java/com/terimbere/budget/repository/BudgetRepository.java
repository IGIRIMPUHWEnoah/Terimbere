package com.terimbere.budget.repository;

import com.terimbere.budget.model.Budget;
import com.terimbere.budget.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUser(User user);
}
