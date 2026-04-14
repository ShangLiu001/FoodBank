-- PrimaryFeed Example Procedure Calls & Verification Queries

USE primaryfeed;

-- ═══════════════════════════════════════════════════════════════════
-- EXAMPLE CALLS
-- ═══════════════════════════════════════════════════════════════════

SELECT 'Calling record_donation: 30 cans of SKU-001 donated to Downtown Boston by donor 1...' AS message;

CALL record_donation(1, 1, 3, 'SKU-001', 30, 'cans', '2027-12-31 00:00:00');

SELECT 'record_donation call completed.' AS message;

-- ─────────────────────────────────────────
-- 1. Verify record_donation worked:
--    Check the new donation header was created
-- ─────────────────────────────────────────
SELECT '[ Verification 1 ] Checking most recent donation header...' AS message;

SELECT
  d.donation_id,
  fb.name          AS food_bank,
  fbb.branch_name,
  dn.donor_name,
  d.donation_date
FROM donations          d
JOIN food_bank_branches fbb ON fbb.branch_id   = d.branch_id
JOIN food_banks         fb  ON fb.food_bank_id = fbb.food_bank_id
JOIN donors             dn  ON dn.donor_id     = d.donor_id
ORDER BY d.donation_id DESC
LIMIT 1;

/*
  Expected output (most recent donation):
  +-------------+-----------------------+-----------------+-------------+----------------+
  | donation_id | food_bank             | branch_name     | donor_name  | donation_date  |
  |-------------|-----------------------|-----------------|-------------|----------------|
  |          11 | Boston Area Food Bank | Downtown Boston | John Carter | 2026-04-01 ... |
  +-------------+-----------------------+-----------------+-------------+----------------+
*/

-- ─────────────────────────────────────────
-- 2. Verify record_donation worked:
--    Check the donation item was linked to the correct inventory batch
-- ─────────────────────────────────────────
SELECT '[ Verification 2 ] Checking donation item linked to inventory batch...' AS message;

SELECT
  di.donation_id,
  di.donation_item_id,
  di.food_sku,
  fi.food_name,
  di.quantity,
  di.expiry_date,
  i.quantity       AS current_inventory_qty
FROM donation_items di
JOIN donations      d  ON d.donation_id  = di.donation_id
JOIN inventories    i  ON i.food_sku     = di.food_sku
                      AND i.branch_id    = d.branch_id
                      AND i.unit         = di.unit
                      AND i.expiry_date  = di.expiry_date
JOIN food_items     fi ON fi.sku         = i.food_sku		
WHERE di.donation_id = (SELECT MAX(donation_id) FROM donations);

/*
  Expected output:
  The donation creates a new inventory batch expiring 2027-12-31 with quantity 30,
  since no existing batch at branch 1 has that expiry date.
  +-------------+------------------+---------+------------------+----------+-----------------------+---------------------+
  | donation_id | donation_item_id | sku     | food_name        | quantity | expiry_date         | current_inventory_qty |
  |-------------|------------------|---------|------------------|----------|-----------------------|---------------------|
  |          11 |               15 | SKU-001 | Canned Chickpeas | 30       | 2027-12-31 00:00:00 |                    30 |
  +-------------+------------------+---------+------------------+----------+-----------------------+---------------------+
*/

-- ─────────────────────────────────────────

SELECT 'Calling record_distribution: distributing 5 cans of SKU-001 at Downtown Boston to beneficiary 1 (FIFO)...' AS message;

CALL record_distribution(1, 1, 5, 'SKU-001', 5);

SELECT 'record_distribution call completed.' AS message;

-- ─────────────────────────────────────────
-- 3. Verify record_distribution worked:
--    Check the distribution header was created
-- ─────────────────────────────────────────
SELECT '[ Verification 3 ] Checking most recent distribution header...' AS message;

SELECT
  d.distribution_id,
  fbb.branch_name,
  b.beneficiary_full_name,
  d.distribution_date
FROM distributions      d
JOIN food_bank_branches fbb ON fbb.branch_id    = d.branch_id
JOIN beneficiaries      b   ON b.beneficiary_id = d.beneficiary_id
ORDER BY d.distribution_id DESC
LIMIT 1;

/*
  Expected output (most recent distribution):
  +-----------------+-----------------+-----------------------+---------------------+
  | distribution_id | branch_name     | beneficiary_full_name | distribution_date   |
  |-----------------|-----------------|-----------------------|---------------------|
  | 10              | Downtown Boston | Linda Park            | 2026-04- ...      |
  +-----------------+-----------------+-----------------------+---------------------+
*/

-- ─────────────────────────────────────────
-- 4. Verify record_distribution worked:
--    Check inventory was decremented from the earliest expiry batch (FIFO)
-- ─────────────────────────────────────────
SELECT '[ Verification 4 ] Checking FIFO inventory decrement for SKU-001 at branch 1...' AS message;

SELECT
  i.inventory_id,
  fi.sku,
  fi.food_name,
  fbb.branch_name,
  i.quantity       AS remaining_qty,
  i.expiry_date
FROM inventories        i
JOIN food_items         fi  ON fi.sku        = i.food_sku
JOIN food_bank_branches fbb ON fbb.branch_id = i.branch_id
WHERE i.food_sku  = 'SKU-001'
  AND i.branch_id = 1
ORDER BY i.expiry_date ASC;

/*
  Expected output:
  FIFO picks inventory_id = 2 (expiry 2026-04-03) first, so that batch is decremented.
  inventory_id = 1 (expiry 2026-12-31) is untouched.
  New batch from donation call has quantity 30.
  +--------------+---------+------------------+-----------------+---------------+---------------------+
  | inventory_id | sku     | food_name        | branch_name     | remaining_qty | expiry_date         |
  |--------------|---------|------------------|-----------------|---------------|---------------------|
  | 2            | SKU-001 | Canned Chickpeas | Downtown Boston | 24            | 2026-04-03 00:00:00 |
  | 1            | SKU-001 | Canned Chickpeas | Downtown Boston | 118           | 2026-12-31 00:00:00 |
  | (new)        | SKU-001 | Canned Chickpeas | Downtown Boston | 30            | 2027-12-31 00:00:00 |
  (batch 2: 29 - 5 distributed = 24)
  +--------------+---------+------------------+-----------------+---------------+---------------------+
*/

-- ─────────────────────────────────────────
-- 5. Verify the below-zero guard works:
--    Attempt to distribute more than available — should raise an error
-- ─────────────────────────────────────────
SELECT '[ Verification 5 ] Testing below-zero guard — expecting an error...' AS message;

CALL record_distribution(1, 1, 3, 'SKU-001', 99999);

/*
  Expected output:
  ERROR 1644 (45000): Insufficient stock: requested quantity would bring inventory below 0.
*/
