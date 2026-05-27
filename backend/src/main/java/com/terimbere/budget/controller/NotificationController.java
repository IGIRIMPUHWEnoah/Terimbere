package com.terimbere.budget.controller;

import com.terimbere.budget.dto.request.NotificationSettingsRequest;
import com.terimbere.budget.model.Notification;
import com.terimbere.budget.model.NotificationSettings;
import com.terimbere.budget.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification Center", description = "Endpoints for retrieving system and email reminder feeds, marking items as read, and managing granular user preferences.")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get user system notification feed", description = "Optional query param unreadOnly: 'true' to fetch only unread notifications, or 'false' for all.")
    public ResponseEntity<List<Notification>> getNotificationFeed(@RequestParam(required = false) Boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getFeedForCurrentUser(unreadOnly));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a specific notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all unread notifications in feed as read")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    @Operation(summary = "Get current user reminder frequency and toggle settings")
    public ResponseEntity<NotificationSettings> getSettings() {
        return ResponseEntity.ok(notificationService.getSettingsForCurrentUser());
    }

    @PutMapping("/settings")
    @Operation(summary = "Update user notification preference thresholds")
    public ResponseEntity<NotificationSettings> updateSettings(@Valid @RequestBody NotificationSettingsRequest request) {
        NotificationSettings settings = notificationService.updateSettings(request);
        return ResponseEntity.ok(settings);
    }
}
