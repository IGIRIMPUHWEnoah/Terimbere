package com.terimbere.budget.service;

import com.terimbere.budget.model.DebtInstallment;
import com.terimbere.budget.model.DebtRecord;
import com.terimbere.budget.model.NotificationSettings;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.DebtInstallmentRepository;
import com.terimbere.budget.repository.DebtRecordRepository;
import com.terimbere.budget.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Dynamic debt scheduler that allows each user to configure their own
 * overdue-debt check time. Instead of a static @Scheduled cron, this
 * component uses Spring's TaskScheduler to dynamically schedule per-user
 * cron jobs. When a user updates their schedule, the old task is cancelled
 * and a new one is registered.
 */
@Component
public class DebtScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DebtScheduler.class);

    private final DebtRecordRepository debtRecordRepository;
    private final DebtInstallmentRepository debtInstallmentRepository;
    private final UserRepository userRepository;
    private final TaskScheduler taskScheduler;

    /** Tracks the active scheduled task per user so we can cancel & reschedule. */
    private final Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DebtScheduler(DebtRecordRepository debtRecordRepository,
                         DebtInstallmentRepository debtInstallmentRepository,
                         UserRepository userRepository,
                         TaskScheduler taskScheduler) {
        this.debtRecordRepository = debtRecordRepository;
        this.debtInstallmentRepository = debtInstallmentRepository;
        this.userRepository = userRepository;
        this.taskScheduler = taskScheduler;
    }

    /**
     * On application startup, register a scheduled task for every existing user
     * based on their stored debtCheckHour/debtCheckMinute preferences.
     */
    @PostConstruct
    public void initSchedules() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            scheduleForUser(user);
        }
        logger.info("Initialized dynamic debt-check schedules for {} users.", allUsers.size());
    }

    /**
     * Schedule (or reschedule) the overdue-debt check for a single user.
     * Call this after the user updates their scheduler preferences.
     */
    public void scheduleForUser(User user) {
        UUID userId = user.getId();

        // Cancel existing task if present
        ScheduledFuture<?> existing = scheduledTasks.remove(userId);
        if (existing != null) {
            existing.cancel(false);
        }

        NotificationSettings settings = user.getNotificationSettings();
        int hour = 0;
        int minute = 0;
        if (settings != null) {
            hour = settings.getDebtCheckHour() != null ? settings.getDebtCheckHour() : 0;
            minute = settings.getDebtCheckMinute() != null ? settings.getDebtCheckMinute() : 0;
        }

        // Build cron expression: "0 {minute} {hour} * * ?" — every day at that time
        String cron = String.format("0 %d %d * * ?", minute, hour);
        logger.info("Scheduling debt-check for user {} at cron [{}]", userId, cron);

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> runOverdueCheckForUser(userId),
                new CronTrigger(cron)
        );
        scheduledTasks.put(userId, future);
    }

    /**
     * Execute the overdue-debt marking logic for a single user.
     */
    @Transactional
    public void runOverdueCheckForUser(UUID userId) {
        logger.info("Running dynamic scheduled debt-check for user {}...", userId);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.warn("User {} not found, skipping debt check.", userId);
            return;
        }

        LocalDate today = LocalDate.now();
        int totalUpdated = 0;

        List<DebtRecord> overdueDebts = debtRecordRepository.findOverdueDebts(user, today);
        for (DebtRecord debt : overdueDebts) {
            debt.setStatus("OVERDUE");
            debtRecordRepository.save(debt);
            totalUpdated++;
        }

        List<DebtInstallment> overdueInstallments = debtInstallmentRepository.findOverdueInstallments(today);
        int totalInstallmentsUpdated = 0;
        for (DebtInstallment inst : overdueInstallments) {
            inst.setStatus("OVERDUE");
            debtInstallmentRepository.save(inst);
            totalInstallmentsUpdated++;
        }

        logger.info("Debt-check for user {} completed. Marked {} debts and {} installments as OVERDUE.",
                userId, totalUpdated, totalInstallmentsUpdated);
    }

    /**
     * Public convenience: trigger the overdue check immediately for all users.
     * Useful for the legacy @Scheduled fallback or manual triggers.
     */
    @Transactional
    public void markOverdueDebts() {
        logger.info("Running scheduled task to check for overdue debts...");
        LocalDate today = LocalDate.now();
        List<User> allUsers = userRepository.findAll();

        int totalUpdated = 0;

        for (User user : allUsers) {
            List<DebtRecord> overdueDebts = debtRecordRepository.findOverdueDebts(user, today);
            for (DebtRecord debt : overdueDebts) {
                debt.setStatus("OVERDUE");
                debtRecordRepository.save(debt);
                totalUpdated++;
            }
        }

        List<DebtInstallment> overdueInstallments = debtInstallmentRepository.findOverdueInstallments(today);
        int totalInstallmentsUpdated = 0;
        for (DebtInstallment inst : overdueInstallments) {
            inst.setStatus("OVERDUE");
            debtInstallmentRepository.save(inst);
            totalInstallmentsUpdated++;
        }

        logger.info("Scheduled task completed. Marked {} debts and {} installments as OVERDUE.", totalUpdated, totalInstallmentsUpdated);
    }
}
