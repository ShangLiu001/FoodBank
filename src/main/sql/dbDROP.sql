-- PrimaryFeed Cleanup Script
-- Drops all triggers, tables, and the database itself for a clean reset.
-- After running this script, re-run in order:
--   1. dbDDL.sql
--   2. dbTRIGGERS.sql
--   3. dbPROCS.sql
--   4. dbDML.sql
--   5. (OPTIONAL) dbPROCSCALL.sql to test-run the created procedures
--   6. dbSQL.sql
-- OR just the provided execute.sql for a full run.

USE primaryfeed;

-- ─────────────────────────────────────────
-- STEP 1: Drop all triggers and procedures
-- Must be dropped before tables since they depend on them
-- ─────────────────────
DROP TRIGGER IF EXISTS trg_after_donation_items_insert;
DROP TRIGGER IF EXISTS trg_staff_check_role;
DROP TRIGGER IF EXISTS trg_volunteers_check_role;
DROP TRIGGER IF EXISTS trg_users_prevent_role_change;
DROP TRIGGER IF EXISTS trg_after_distribution_items_insert;

DROP PROCEDURE IF EXISTS record_donation;
DROP PROCEDURE IF EXISTS record_distribution;

-- ─────────────────────────────────────────
-- STEP 2: Disable FK checks so tables can be
--         dropped without worrying about order
-- ─────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 0;

-- ─────────────────────────────────────────
-- STEP 3: Drop all tables
-- ─────────────────────────────────────────
DROP TABLE IF EXISTS trigger_logs;
DROP TABLE IF EXISTS distribution_items;
DROP TABLE IF EXISTS distributions;
DROP TABLE IF EXISTS donation_items;
DROP TABLE IF EXISTS donations;
DROP TABLE IF EXISTS beneficiaries;
DROP TABLE IF EXISTS donors;
DROP TABLE IF EXISTS volunteer_shifts;
DROP TABLE IF EXISTS inventories;
DROP TABLE IF EXISTS food_items;
DROP TABLE IF EXISTS food_categories;
DROP TABLE IF EXISTS user_admin_permissions;
DROP TABLE IF EXISTS admin_permissions;
DROP TABLE IF EXISTS staff;
DROP TABLE IF EXISTS volunteers;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS food_bank_branches;
DROP TABLE IF EXISTS food_banks;
DROP TABLE IF EXISTS addresses;

-- ─────────────────────────────────────────
-- STEP 4: Re-enable FK checks
-- ─────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 1;

-- ─────────────────────────────────────────
-- STEP 5: Drop the database itself
-- ─────────────────────────────────────────
DROP DATABASE IF EXISTS primaryfeed;
