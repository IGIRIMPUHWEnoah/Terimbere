package com.terimbere.budget.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures budget portal columns exist on existing PostgreSQL databases.
 * Hibernate ddl-auto=update does not always add new columns to existing tables.
 */
@Component
public class DatabaseSchemaUpdater implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaUpdater.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        applyBudgetPortalColumns();
        applySchedulerColumns();
    }

    private void applyBudgetPortalColumns() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE budgets ADD COLUMN IF NOT EXISTS budget_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL'"
            );
            jdbcTemplate.execute("ALTER TABLE budgets ADD COLUMN IF NOT EXISTS notes TEXT");
            jdbcTemplate.execute(
                    "ALTER TABLE budgets ADD COLUMN IF NOT EXISTS savings_goal NUMERIC(15, 2)"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE budgets ADD COLUMN IF NOT EXISTS project_total_budget NUMERIC(15, 2)"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE budget_entries ADD COLUMN IF NOT EXISTS amount_saved NUMERIC(15, 2)"
            );
            log.info("Budget portal database columns verified.");
        } catch (Exception ex) {
            log.error("Failed to apply budget portal schema updates: {}", ex.getMessage());
            throw ex;
        }
    }

    private void applySchedulerColumns() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE notification_settings ADD COLUMN IF NOT EXISTS debt_check_hour INTEGER DEFAULT 0"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE notification_settings ADD COLUMN IF NOT EXISTS debt_check_minute INTEGER DEFAULT 0"
            );
            log.info("Scheduler database columns verified.");
        } catch (Exception ex) {
            log.error("Failed to apply scheduler schema updates: {}", ex.getMessage());
            throw ex;
        }
    }
}
