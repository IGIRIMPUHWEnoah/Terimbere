package com.terimbere.budget.repository;

import com.terimbere.budget.model.Budget;
import com.terimbere.budget.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUser(User user);

    @Query("SELECT DISTINCT b FROM Budget b LEFT JOIN FETCH b.entries WHERE b.user = :user ORDER BY b.createdAt DESC")
    List<Budget> findByUserWithEntries(@Param("user") User user);

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.entries WHERE b.id = :id")
    Optional<Budget> findByIdWithEntries(@Param("id") UUID id);
}
