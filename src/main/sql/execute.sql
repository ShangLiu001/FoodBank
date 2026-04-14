-- ───────────────────────────────────────
-- Drop all tables and database if exists
-- ───────────────────────────────────────
SELECT '(Clean up all tables and database if any.)' as message;
source dbDROP.sql;

-- ───────────────────────────────────────
-- Create database and tables
-- ───────────────────────────────────────
SELECT 'Create database PrimaryFeed and tables...' as message;
source dbDDL.sql;

-- ───────────────────────────────────────
-- Seed sample data
-- ───────────────────────────────────────
SELECT 'Seed tables with sample data...' as message;
source dbDML.sql;

SELECT 'addresses' AS table_name; SELECT * FROM addresses;
SELECT 'food_banks' AS table_name; SELECT * FROM food_banks;
SELECT 'food_bank_branches' AS table_name; SELECT * FROM food_bank_branches;
SELECT 'users' AS table_name; SELECT * FROM users;
SELECT 'volunteers' AS table_name; SELECT * FROM volunteers;
SELECT 'volunteer_shifts' AS table_name; SELECT * FROM volunteer_shifts;
SELECT 'staff' AS table_name; SELECT * FROM staff;
SELECT 'admin_permissions' AS table_name; SELECT * FROM admin_permissions;
SELECT 'admin_permissions_per_user' AS table_name;
SELECT
  u.user_id,
  u.first_name,
  u.last_name,
  CASE u.role
    WHEN 0 THEN 'Staff'
    WHEN 1 THEN 'Volunteer'
  END AS user_type,
  s.job_title,
  GROUP_CONCAT(
    ap.permission_description
    ORDER BY ap.permission_type
    SEPARATOR ' | '
  ) AS permissions
FROM users u
JOIN user_admin_permissions uap ON uap.user_id = u.user_id
JOIN admin_permissions      ap  ON ap.permission_id = uap.permission_id
LEFT JOIN staff             s   ON s.user_id = u.user_id
GROUP BY
  u.user_id,
  u.first_name,
  u.last_name,
  u.role,
  s.job_title
ORDER BY u.user_id;
SELECT 'food_categories' AS table_name; SELECT * FROM food_categories;
SELECT 'food_items' AS table_name; SELECT * FROM food_items;
SELECT 'donors' AS table_name; SELECT * FROM donors;
SELECT 'donations' AS table_name; SELECT * FROM donations;
SELECT 'donation_items' AS table_name; SELECT * FROM donation_items;
SELECT 'inventories' AS table_name; SELECT * FROM inventories;
SELECT 'beneficiaries' AS table_name; SELECT * FROM beneficiaries;
SELECT 'distributions' AS table_name; SELECT * FROM distributions;
SELECT 'distribution_items' AS table_name; SELECT * FROM distribution_items;
SELECT 'trigger_logs' AS table_name; SELECT * FROM trigger_logs;

-- ─────────────────────────────────────────
-- TEST OUT VIEWS
-- ─────────────────────────────────────────
SELECT 'Testing out views...' as message;

SELECT 'Inventory items expiring within next 3 months' as description;
SELECT * from vw_expiring_inventory;
/*
  Expected output:
  +--------------+-----------------+----------+-----------------+----------+--------+---------------------+-------------------+
| inventory_id | branch_name     | food_sku | food_name       | quantity | unit   | expiry_date         | days_until_expiry |
+--------------+-----------------+----------+-----------------+----------+--------+---------------------+-------------------+
|           13 | Dorchester      | SKU-005  | Chicken Breast  |       29 | lbs    | 2026-06-01 00:00:00 |                52 |
|            6 | Roxbury         | SKU-003  | Russet Potatoes |       50 | bags   | 2026-05-01 00:00:00 |                21 |
|            5 | Roxbury         | SKU-003  | Russet Potatoes |       25 | bags   | 2026-05-15 00:00:00 |                35 |
|            4 | Downtown Boston | SKU-002  | Whole Milk (1L) |       25 | liters | 2026-04-10 00:00:00 |                 0 |
+--------------+-----------------+----------+-----------------+----------+--------+---------------------+-------------------+
*/

SELECT 'Hours worked by volunteers' as description;
SELECT * from vw_volunteer_hours_log_last_30_days;
/*
  Expected output:
  +----------+--------------+------------+-----------+-----------+------------+------------------+----------------+-------------+
| shift_id | volunteer_id | first_name | last_name | branch_id | shift_date | shift_time_start | shift_time_end | total_hours |
+----------+--------------+------------+-----------+-----------+------------+------------------+----------------+-------------+
|        1 |            1 | Carol      | Lee       |         1 | 2026-04-05 | 09:00:00         | 12:00:00       | 3h 0m       |
|        2 |            1 | Carol      | Lee       |         1 | 2026-04-12 | 09:00:00         | 12:00:00       | 3h 0m       |
|        8 |            1 | Carol      | Lee       |         2 | 2026-04-19 | 09:00:00         | 12:00:00       | 3h 0m       |
|        3 |            2 | Eva        | Patel     |         3 | 2026-04-05 | 08:00:00         | 11:00:00       | 3h 0m       |
|        4 |            2 | Eva        | Patel     |         3 | 2026-04-07 | 08:00:00         | 11:00:00       | 3h 0m       |
|        9 |            2 | Eva        | Patel     |         4 | 2026-04-10 | 13:00:00         | 17:00:00       | 4h 0m       |
|        5 |            3 | Grace      | Wang      |         6 | 2026-04-06 | 10:00:00         | 14:00:00       | 4h 0m       |
|        6 |            3 | Grace      | Wang      |         6 | 2026-04-13 | 10:00:00         | 14:00:00       | 4h 0m       |
+----------+--------------+------------+-----------+-----------+------------+------------------+----------------+-------------+
*/

-- ───────────────────────────────────────
-- Sample SQL queries
-- ───────────────────
SELECT 'Running sample queries...' as message;
source dbSQL.sql;

-- ───────────────────────────────────────
-- Sample procedure calls
-- ───────────────────────────────────────
SELECT 'Test calling procedures...' as message;
source dbPROCSCALL.sql;
