package com.terimbere.budget.repository;

import com.terimbere.budget.model.IncomeSource;
import com.terimbere.budget.model.IncomePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncomeSourceRepository extends JpaRepository<IncomeSource, UUID> {
    List<IncomeSource> findByIncomePlan(IncomePlan incomePlan);
}
