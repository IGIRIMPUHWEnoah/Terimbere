package com.terimbere.budget.repository;

import com.terimbere.budget.model.Notification;
import com.terimbere.budget.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndReadStatusOrderByCreatedAtDesc(User user, boolean readStatus);

    @Query("SELECT n FROM Notification n WHERE n.sentAt IS NULL AND n.scheduledAt <= :now")
    List<Notification> findPendingNotificationsToDeliver(@Param("now") LocalDateTime now);
}
