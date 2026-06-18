package com.terimbere.budget.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Provides a TaskScheduler bean used by DebtScheduler for dynamic per-user cron scheduling.
 */
@Configuration
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("debt-scheduler-");
        scheduler.setErrorHandler(t -> org.slf4j.LoggerFactory.getLogger("DebtScheduler")
                .error("Error in scheduled debt check task", t));
        scheduler.initialize();
        return scheduler;
    }
}
