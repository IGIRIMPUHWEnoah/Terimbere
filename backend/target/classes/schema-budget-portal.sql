-- Run this once if budgets API fails with "column budget_type does not exist"
-- psql -U postgres -d terimbere_db -f schema-budget-portal.sql

ALTER TABLE budgets ADD COLUMN IF NOT EXISTS budget_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL';
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS savings_goal NUMERIC(15, 2);
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS project_total_budget NUMERIC(15, 2);

ALTER TABLE budget_entries ADD COLUMN IF NOT EXISTS amount_saved NUMERIC(15, 2);
