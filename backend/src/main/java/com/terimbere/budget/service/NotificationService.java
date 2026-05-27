package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.NotificationSettingsRequest;
import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.*;
import com.terimbere.budget.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final BillRepository billRepository;
    private final DebtRecordRepository debtRecordRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationSettingsRepository settingsRepository,
                               BillRepository billRepository,
                               DebtRecordRepository debtRecordRepository,
                               ContractRepository contractRepository,
                               UserRepository userRepository,
                               AuthService authService) {
        this.notificationRepository = notificationRepository;
        this.settingsRepository = settingsRepository;
        this.billRepository = billRepository;
        this.debtRecordRepository = debtRecordRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<Notification> getFeedForCurrentUser(Boolean unreadOnly) {
        User user = authService.getCurrentAuthenticatedUser();
        if (unreadOnly != null && unreadOnly) {
            return notificationRepository.findByUserAndReadStatusOrderByCreatedAtDesc(user, false);
        }
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        User user = authService.getCurrentAuthenticatedUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to notification");
        }

        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User user = authService.getCurrentAuthenticatedUser();
        List<Notification> unread = notificationRepository.findByUserAndReadStatusOrderByCreatedAtDesc(user, false);
        for (Notification n : unread) {
            n.setReadStatus(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public NotificationSettings getSettingsForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return settingsRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
    }

    @Transactional
    public NotificationSettings updateSettings(NotificationSettingsRequest request) {
        NotificationSettings settings = getSettingsForCurrentUser();
        settings.setEmailNotifications(request.getEmailNotifications());
        settings.setInAppNotifications(request.getInAppNotifications());
        settings.setDaysBeforeBillReminder(request.getDaysBeforeBillReminder());
        settings.setDaysBeforeContractExpiry(request.getDaysBeforeContractExpiry());
        return settingsRepository.save(settings);
    }

    @Transactional
    public void createSystemNotification(User user, String title, String message, String type, UUID refId) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .referenceId(refId)
                .scheduledAt(LocalDateTime.now())
                .readStatus(false)
                .sentAt(LocalDateTime.now()) // immediately sent
                .build();
        
        notificationRepository.save(notification);
    }

    // --- SCHEDULED REMINDERS JOB (Runs every day at midnight or 1 hour interval for demo) ---
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void runScheduledRemindersJob() {
        List<User> users = userRepository.findAll();
        LocalDate today = LocalDate.now();

        for (User user : users) {
            NotificationSettings settings = user.getNotificationSettings();
            if (settings == null) continue;

            // 1. Check upcoming bills
            int billDays = settings.getDaysBeforeBillReminder();
            LocalDate billTargetDate = today.plusDays(billDays);
            List<Bill> upcomingBills = billRepository.findUpcomingUnpaidBills(user, billTargetDate);
            for (Bill bill : upcomingBills) {
                String title = "Upcoming Bill Obligation: " + bill.getTitle();
                String message = String.format("A payment of %s for '%s' is due on %s. Please settle it in time to avoid fines.",
                        bill.getAmount(), bill.getTitle(), bill.getDueDate());
                
                // Avoid duplicating notification if already sent recently
                if (!checkNotificationExists(user, bill.getId(), "BILL_REMINDER")) {
                    createSystemNotification(user, title, message, "BILL_REMINDER", bill.getId());
                }
            }

            // 2. Check overdue debts
            List<DebtRecord> overdueDebts = debtRecordRepository.findOverdueDebts(user, today);
            for (DebtRecord debt : overdueDebts) {
                String name = debt.getContact().getFullName();
                String title = debt.getDebtDirection().equals("THEY_OWE_ME") ? "Debtor Alert: " + name : "Creditor Obligation: " + name;
                String message = debt.getDebtDirection().equals("THEY_OWE_ME") 
                        ? String.format("%s owes you %s which was due on %s. Click here to trigger an SMS/WhatsApp reminder.",
                            name, debt.getRemainingAmount(), debt.getDueDate())
                        : String.format("You owe %s an amount of %s which was due on %s. Please settle to maintain trust.",
                            name, debt.getRemainingAmount(), debt.getDueDate());

                if (!checkNotificationExists(user, debt.getId(), "DEBT_REMINDER")) {
                    createSystemNotification(user, title, message, "DEBT_REMINDER", debt.getId());
                }
            }

            // 3. Check expiring contracts
            int contractDays = settings.getDaysBeforeContractExpiry();
            LocalDate contractTargetDate = today.plusDays(contractDays);
            List<Contract> contracts = contractRepository.findByUser(user);
            for (Contract contract : contracts) {
                if (contract.getEndDate() != null && !contract.getEndDate().isBefore(today) && !contract.getEndDate().isAfter(contractTargetDate)) {
                    String title = "Contract Expiry Warning: " + contract.getTitle();
                    String message = String.format("The contract '%s' with %s is set to expire on %s. Please review renewal options.",
                            contract.getTitle(), contract.getContact().getFullName(), contract.getEndDate());

                    if (!checkNotificationExists(user, contract.getId(), "CONTRACT_EXPIRY")) {
                        createSystemNotification(user, title, message, "CONTRACT_EXPIRY", contract.getId());
                    }
                }
            }
        }
    }

    private boolean checkNotificationExists(User user, UUID refId, String type) {
        List<Notification> notifs = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        return notifs.stream().anyMatch(n -> 
                n.getReferenceId() != null 
                && n.getReferenceId().equals(refId) 
                && n.getNotificationType().equalsIgnoreCase(type)
                && n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(3)) // ignore older than 3 days
        );
    }
}
