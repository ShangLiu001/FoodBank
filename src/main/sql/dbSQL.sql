-- PrimaryFeed SQL Queries
-- Based on relational algebra formulations from the project specification
-- Each query includes: English description, SQL, and expected output

USE primaryfeed;

-- ═══════════════════════════════════════════════════════════════════
-- QUERY 1 (RA #1): Food items currently available at a specific branch
-- JOIN query across 3 relations: inventories, food_items, food_bank_branches
-- ═══════════════════════════════════════════════════════════════════

SELECT 'Show all food items with available stock (quantity > 0) at the Downtown Boston branch (branch_id = 1), including the food name, SKU, quantity, unit, and expiry date.' as 'Query #1';

SELECT
  fi.sku,
  fi.food_name,
  i.quantity,
  i.unit,
  i.expiry_date,
  fbb.branch_name
FROM inventories        i
JOIN food_items         fi  ON fi.sku        = i.food_sku
JOIN food_bank_branches fbb ON fbb.branch_id = i.branch_id
WHERE i.branch_id = 1
  AND i.quantity  > 0
ORDER BY i.expiry_date ASC;

/*
  Expected output (branch_id = 1 has SKU-001 in two batches, and SKU-002):
  +---------+------------------+----------+--------+---------------------+-----------------+
  | sku     | food_name        | quantity | unit   | expiry_date         | branch_name     |
  |---------|------------------|----------|--------|---------------------|-----------------|
  | SKU-001 | Canned Chickpeas |       29 | cans   | 2026-04-03 00:00:00 | Downtown Boston |
  | SKU-002 | Whole Milk (1L)  |       25 | liters | 2026-04-10 00:00:00 | Downtown Boston |
  | SKU-011 | Canned Tomatoes  |       57 | cans   | 2026-10-01 00:00:00 | Downtown Boston |
  | SKU-001 | Canned Chickpeas |      118 | cans   | 2026-12-31 00:00:00 | Downtown Boston |
  +---------+------------------+----------+--------+---------------------+-----------------+
*/


-- ═══════════════════════════════════════════════════════════════════
-- QUERY 2 (RA #2): Food items expiring within the next 3 days
-- JOIN query across 3 relations: inventories, food_items, food_bank_branches
-- ═══════════════════════════════════════════════════════════════════

SELECT 'List all food items across all branches whose expiry date falls within the next 3 days from today, showing branch name, SKU, food name, quantity, and expiry date. Order by expiry date ascending so the most urgent items appear first.' as 'Query #2';

SELECT
  fbb.branch_name,
  fi.sku,
  fi.food_name,
  i.quantity,
  i.unit,
  i.expiry_date
FROM inventories        i
JOIN food_items         fi  ON fi.sku        = i.food_sku
JOIN food_bank_branches fbb ON fbb.branch_id = i.branch_id
WHERE i.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 3 DAY)
  AND i.quantity     > 0
ORDER BY i.expiry_date ASC;

/*
  Expected output (assuming today is 2026-04-01, SKU-001 old batch expires 2026-04-03):
  +-----------------+---------+------------------+----------+--------+---------------------+
  | branch_name     | sku     | food_name        | quantity | unit   | expiry_date         |
  |-----------------|---------|------------------|----------|--------|---------------------|
  | Downtown Boston | SKU-002 | Whole Milk (1L)  | 25       | liters | 2026-04-10 00:00:00 |
  +-----------------+---------+------------------+----------+--------+---------------------+
*/


-- ═══════════════════════════════════════════════════════════════════
-- QUERY 3 (RA #4 + #15): Food item count and total distributed quantity
-- by category — AGGREGATE query with GROUP BY, HAVING, and ORDER BY
-- Joins through distribution_items → inventories → food_items to get SKU
-- ═══════════════════════════════════════════════════════════════════

SELECT 'For each food category that has more than 1 food item registered, show the category name, how many distinct food items belong to it, and the total quantity that has been distributed across all branches. Order by total distributed quantity descending so the most in-demand categories appear first.' as 'Query #3';

SELECT
  fc.category_name,
  COUNT(DISTINCT fi.sku) AS item_count,
  SUM(di.quantity)       AS total_distributed
FROM food_categories    fc
JOIN food_items         fi  ON fi.category_id  = fc.category_id
JOIN inventories        i   ON i.food_sku      = fi.sku
JOIN distribution_items di  ON di.inventory_id = i.inventory_id

WHERE fc.category_id IN (
  SELECT category_id
  FROM food_items
  GROUP BY category_id
  HAVING COUNT(sku) > 1
)
GROUP BY fc.category_id, fc.category_name
ORDER BY total_distributed DESC;

/*
  Expected output:
  +---------------+------------+-------------------+
  | category_name | item_count | total_distributed |
  |---------------|------------|-------------------|
  | Canned Goods  |          2 |                14 |
  +---------------+------------+-------------------+
*/


-- ═══════════════════════════════════════════════════════════════════
-- QUERY 4 (RA #7): Total volunteer hours worked per volunteer
-- in the last 30 days — AGGREGATE query with GROUP BY, HAVING, ORDER BY
-- ═══════════════════════════════════════════════════════════════════

SELECT 'For each volunteer who has worked at least one shift in the last 30 days, show their full name, total number of shifts worked, and total hours worked. Only include volunteers who have worked more than 2 hours in total. Order by total hours descending.' as 'Query #4';

SELECT
  u.first_name,
  u.last_name,
  COUNT(vs.shift_id)                                                 AS shifts_worked,
  SUM(TIMESTAMPDIFF(HOUR, vs.shift_time_start, vs.shift_time_end))  AS total_hours
FROM volunteer_shifts vs
JOIN volunteers       v  ON v.volunteer_id = vs.volunteer_id
JOIN users            u  ON u.user_id      = v.user_id
WHERE vs.shift_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY v.volunteer_id, u.first_name, u.last_name
HAVING total_hours > 2
ORDER BY total_hours DESC;

/*
  Expected output:
  +------------+-----------+---------------+-------------+
  | first_name | last_name | shifts_worked | total_hours |
  |------------|-----------|---------------|-------------|
  | Carol      | Lee       | 4             | 12          |
  | Eva        | Patel     | 3             | 10          |
  | Grace      | Wang      | 2             | 8           |
  +------------+-----------+---------------+-------------+
*/


-- ═══════════════════════════════════════════════════════════════════
-- QUERY 5 (RA #12): Total food received vs distributed per branch
-- JOIN across 5 relations with subquery (inline views)
-- Joins through inventories to get food_sku from donation/distribution items
-- ═══════════════════════════════════════════════════════════════════

SELECT 'For each branch, compare the total quantity of food received through donations versus the total quantity distributed to beneficiaries. Show the branch name, food bank name, total received, total distributed, and the net surplus (received minus distributed). Order by surplus descending.' as 'Query #5';

SELECT
  fbb.branch_name,
  fb.name                                    AS food_bank_name,
  COALESCE(received.total_received, 0)       AS total_received,
  COALESCE(distributed.total_distributed, 0) AS total_distributed,
  COALESCE(received.total_received, 0)
    - COALESCE(distributed.total_distributed, 0) AS surplus
FROM food_bank_branches fbb
JOIN food_banks         fb ON fb.food_bank_id = fbb.food_bank_id

-- Inline view: total received per branch via donation_items → donations
LEFT JOIN (
  SELECT
    d.branch_id,
    SUM(di.quantity) AS total_received
  FROM donation_items di
  JOIN donations      d ON d.donation_id = di.donation_id
  GROUP BY d.branch_id
) received ON received.branch_id = fbb.branch_id

-- Inline view: total distributed per branch via distribution_items → inventories
LEFT JOIN (
  SELECT
    i.branch_id,
    SUM(di.quantity) AS total_distributed
  FROM distribution_items di
  JOIN inventories        i ON i.inventory_id = di.inventory_id
  GROUP BY i.branch_id
) distributed ON distributed.branch_id = fbb.branch_id

ORDER BY surplus DESC;

/*
  Expected output:
  +-----------------+--------------------------+----------------+-------------------+---------+
  | branch_name     | food_bank_name           | total_received | total_distributed | surplus |
  |-----------------|--------------------------|----------------|-------------------|---------|
  | Roxbury         | Boston Area Food Bank    |            275 |                 3 |     272 |
  | Downtown Boston | Boston Area Food Bank    |            248 |                19 |     229 |
  | Dorchester      | Boston Area Food Bank    |            110 |                 5 |     105 |
  | Lynn North      | Greater Lynn Food Pantry |            100 |                 2 |      98 |
  | Lynn Harbor     | Greater Lynn Food Pantry |             75 |                 6 |      69 |
  | Jamaica Plain   | Boston Area Food Bank    |             60 |                 0 |      60 |
  | Lynn Central    | Greater Lynn Food Pantry |             45 |                 5 |      40 |
  | Lynn Woods      | Greater Lynn Food Pantry |             12 |                 3 |       9 |
  | South End       | Boston Area Food Bank    |              0 |                 0 |       0 |
  | Swampscott      | Greater Lynn Food Pantry |              0 |                 0 |       0 |
  +-----------------+--------------------------+----------------+-------------------+---------+
*/


-- ═══════════════════════════════════════════════════════════════════
-- QUERY 6 (RA #5): Branch that distributed the most food last month
-- Subquery using WITH (CTE) clause
-- ═══════════════════════════════════════════════════════════════════

SELECT 'Find the branch that distributed the highest total quantity of food in the last 1 month. Show the branch name, food bank name, and total quantity distributed. Use a CTE to first compute totals per branch, then select the one with the maximum total.' as 'Query #6';

WITH branch_totals AS (
  SELECT
    i.branch_id,
    SUM(di.quantity) AS total_distributed
  FROM distributions      d
  JOIN distribution_items di ON di.distribution_id = d.distribution_id
  JOIN inventories        i  ON i.inventory_id     = di.inventory_id
  WHERE d.distribution_date >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)
  GROUP BY i.branch_id
)
SELECT
  fbb.branch_name,
  fb.name AS food_bank_name,
  bt.total_distributed
FROM branch_totals      bt
JOIN food_bank_branches fbb ON fbb.branch_id   = bt.branch_id
JOIN food_banks         fb  ON fb.food_bank_id = fbb.food_bank_id
WHERE bt.total_distributed = (
  SELECT MAX(total_distributed) FROM branch_totals
);

/*
  Expected output:
  +-----------------+-----------------------+-------------------+
  | branch_name     | food_bank_name        | total_distributed |
  |-----------------|-----------------------|-------------------|
  | Downtown Boston | Boston Area Food Bank |                 7 |
  +-----------------+-----------------------+-------------------+
*/


-- ═══════════════════════════════════════════════════════════════════
-- QUERY 7 (RA #17): Branches with highest volunteer-to-distribution ratio
-- Multi-relation JOIN with inline views and ORDER BY
-- ═══════════════════════════════════════════════════════════════════

SELECT 'For each branch, calculate the ratio of active volunteers assigned to that branch versus the total number of distribution transactions recorded at that branch. Show branches that have at least 1 volunteer and 1 distribution, ordered by ratio descending so the most volunteer-rich branches appear first.' as 'Query #7';

SELECT
  fbb.branch_name,
  fb.name                                               AS food_bank_name,
  vol.volunteer_count,
  dist.distribution_count,
  ROUND(vol.volunteer_count / dist.distribution_count, 2) AS ratio
FROM food_bank_branches fbb
JOIN food_banks         fb   ON fb.food_bank_id  = fbb.food_bank_id

-- Inline view: count volunteers per branch via users table
JOIN (
  SELECT
    u.branch_id,
    COUNT(v.volunteer_id) AS volunteer_count
  FROM volunteers v
  JOIN users      u ON u.user_id = v.user_id
  GROUP BY u.branch_id
) vol ON vol.branch_id = fbb.branch_id

-- Inline view: count distributions per branch
JOIN (
  SELECT
    branch_id,
    COUNT(distribution_id) AS distribution_count
  FROM distributions
  GROUP BY branch_id
) dist ON dist.branch_id = fbb.branch_id

ORDER BY ratio DESC;

/*
  Expected output:
  +-----------------+--------------------------+-----------------+--------------------+-------+
  | branch_name     | food_bank_name           | volunteer_count | distribution_count | ratio |
  |-----------------|--------------------------|-----------------|--------------------|-------|
  | Lynn Central    | Greater Lynn Food Pantry |               1 |                  1 |  1.00 |
  | Lynn North      | Greater Lynn Food Pantry |               1 |                  1 |  1.00 |
  | Roxbury         | Boston Area Food Bank    |               1 |                  2 |  0.50 |
  +-----------------+--------------------------+-----------------+--------------------+-------+
*/
