package com.terimbere.budget.repository;

import com.terimbere.budget.model.Contact;
import com.terimbere.budget.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    List<Contact> findByUser(User user);
}
