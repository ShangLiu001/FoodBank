-- PrimaryFeed Stored Procedures
-- Procedures:
--   1. record_donation: inserts a donation + donation_item, increments or creates inventory batch
--   2. record_distribution: inserts a distribution + distribution_item, decrements inventory batch

USE primaryfeed;

DELIMITER $$

-- ═══════════════════════════════════════════════════════════════════
-- PROCEDURE 1: record_donation
--
-- Records a food donation at a branch and increments the inventory
-- for that food item. If a batch with the same SKU and expiry date
-- already exists at the branch, its quantity is incremented.
-- Otherwise a new inventory batch row is created.
--
-- Parameters:
--   p_branch_id:  branch receiving the donation
--   p_donor_id: donor making the donation
--   p_staff_id: staff member recording the donation
--   p_food_sku: SKU of the donated food item
--   p_quantity: quantity donated
--   p_unit: unit of measurement (e.g. 'cans', 'bags')
--   p_expiry_date: expiry date of the donated batch
-- ═══════════════════════════════════════════════════════════════════
DROP PROCEDURE IF EXISTS record_donation$$

CREATE PROCEDURE record_donation(
  IN p_branch_id   INT,
  IN p_donor_id    INT,
  IN p_user_id    INT,
  IN p_food_sku    VARCHAR(45),
  IN p_quantity    INT,
  IN p_unit        VARCHAR(45),
  IN p_expiry_date DATETIME
)
BEGIN
  DECLARE v_donation_id  INT;

  -- Roll back everything if any step fails
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

    -- Step 1: Create the donation header
    INSERT INTO donations (branch_id, donor_id, user_id, donation_date)
    VALUES (p_branch_id, p_donor_id, p_user_id, NOW());

    SET v_donation_id = LAST_INSERT_ID();

    -- Step 2: Insert donation item — trigger handles inventory automatically
    INSERT INTO donation_items (donation_id, food_sku, quantity, unit, expiry_date)
    VALUES (v_donation_id, p_food_sku, p_quantity, p_unit, p_expiry_date);

  COMMIT;
END$$


-- ═══════════════════════════════════════════════════════════════════
-- PROCEDURE 2: record_distribution
--
-- Records a food distribution to a beneficiary and decrements the
-- inventory using FIFO — automatically selects the batch with the
-- earliest expiry date for the given SKU at the given branch.
-- Raises an error if the requested quantity exceeds available stock
-- in the oldest batch, or if no batch is found.
--
-- Parameters:
--   p_branch_id: branch performing the distribution
--   p_beneficiary_id:  beneficiary receiving the food
--   p_staff_id: staff member recording the distribution
--   p_food_sku: SKU of the food item being distributed
--   p_quantity: quantity to distribute
-- ═══════════════════════════════════════════════════════════════════
DROP PROCEDURE IF EXISTS record_distribution$$

CREATE PROCEDURE record_distribution(
  IN p_branch_id      INT,
  IN p_beneficiary_id INT,
  IN p_user_id       INT,
  IN p_food_sku       VARCHAR(45),
  IN p_quantity       INT
)
BEGIN
  DECLARE v_distribution_id INT;
  DECLARE v_inventory_id    INT;
  DECLARE v_available_qty   INT;

  -- Roll back everything if any step fails
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

    -- Step 1: Find the oldest batch (earliest expiry) for the SKU at this branch
    SELECT inventory_id, quantity
    INTO v_inventory_id, v_available_qty
    FROM inventories
    WHERE food_sku  = p_food_sku
      AND branch_id = p_branch_id
      AND quantity  > 0
    ORDER BY expiry_date ASC
    LIMIT 1;

    -- Step 2: Raise an error if no batch found or stock would go below 0
    IF v_inventory_id IS NULL THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No available inventory batch found for the given branch and SKU.';
    END IF;

    IF v_available_qty < p_quantity THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient stock: requested quantity would bring inventory below 0.';
    END IF;

    -- Step 3: Create the distribution header
    INSERT INTO distributions (branch_id, beneficiary_id, user_id, distribution_date)
    VALUES (p_branch_id, p_beneficiary_id, p_user_id, NOW());

    SET v_distribution_id = LAST_INSERT_ID();

    -- Step 4: Record the distribution item linked to the selected inventory batch
    INSERT INTO distribution_items (distribution_id, quantity, inventory_id)
    VALUES (v_distribution_id, p_quantity, v_inventory_id);

  COMMIT;
END$$

DELIMITER ;
