package com.terimbere.budget.repository;

import com.terimbere.budget.model.IncomePlan;
import com.terimbere.budget.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncomePlanRepository extends JpaRepository<IncomePlan, UUID> {
    List<IncomePlan> findByUser(User user);
}
