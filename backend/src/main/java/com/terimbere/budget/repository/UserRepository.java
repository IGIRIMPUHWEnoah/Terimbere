package com.terimbere.budget.repository;

import com.terimbere.budget.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Added to demonstrate Native Query usage
    @Query(value = "SELECT COUNT(*) FROM users", nativeQuery = true)
    long countAllUsersNative();
}
