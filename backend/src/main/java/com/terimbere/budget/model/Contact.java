package com.terimbere.budget.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "contacts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_type", nullable = false, length = 20)
    private String contactType; // ENUM: DEBTOR, CREDITOR, BOTH

    @Column(columnDefinition = "TEXT")
    private String notes;
}
