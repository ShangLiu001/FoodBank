-- PrimaryFeed Triggers
-- Enforce subtype integrity: a user's role must match
-- the subtype table they are inserted into.
-- role: 0=Staff, 1=Volunteer
-- NOTE: MySQL does not support CREATE TRIGGER IF NOT EXISTS.
--       DROP TRIGGER IF EXISTS is used before each definition
--       to safely recreate triggers without errors on re-runs.

USE primaryfeed;

DELIMITER $$

-- ─────────────────────────────────────────
-- TRIGGER 1: Automatically increments or creates an inventory batch when
-- a new donation item is inserted.
-- ─────────────────────────────────────────

DROP TRIGGER IF EXISTS trg_after_donation_items_insert$$

CREATE TRIGGER trg_after_donation_items_insert
AFTER INSERT ON donation_items
FOR EACH ROW
BEGIN
  IF EXISTS (
    SELECT 1
    FROM inventories i
    JOIN donations   d ON d.donation_id = NEW.donation_id
    WHERE i.food_sku    = NEW.food_sku
      AND i.branch_id   = d.branch_id
      AND i.unit        = NEW.unit
      AND i.expiry_date = NEW.expiry_date
  ) THEN
    -- Batch exists: increment quantity
    UPDATE inventories i
    JOIN donations d ON d.donation_id = NEW.donation_id
    SET i.quantity = i.quantity + NEW.quantity
    WHERE i.food_sku    = NEW.food_sku
      AND i.branch_id   = d.branch_id
      AND i.unit        = NEW.unit
      AND i.expiry_date = NEW.expiry_date;
    INSERT INTO trigger_logs (trigger_name, message)
    VALUES ('trg_after_donation_items_insert', CONCAT('Incremented inventory for SKU: ', NEW.food_sku));
  ELSE
    -- No matching batch: create a new inventory row
    INSERT INTO inventories (food_sku, branch_id, quantity, unit, expiry_date)
    SELECT NEW.food_sku, d.branch_id, NEW.quantity, NEW.unit, NEW.expiry_date
    FROM donations d
    WHERE d.donation_id = NEW.donation_id;
    INSERT INTO trigger_logs (trigger_name, message)
    VALUES ('trg_after_donation_items_insert', CONCAT('Created new inventory batch for SKU: ', NEW.food_sku));
  END IF;
END$$

-- ─────────────────────────────────────────
-- TRIGGER 2: validate role before inserting into staff
-- Only users with role=0 may have a row in the staff table
-- ─────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_staff_check_role$$

CREATE TRIGGER trg_staff_check_role
BEFORE INSERT ON staff
FOR EACH ROW
BEGIN
  DECLARE user_role INT;
  SELECT role INTO user_role FROM users WHERE user_id = NEW.user_id;

  IF user_role IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot insert into staff: user does not exist.';
  END IF;

  IF user_role != 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot insert into staff: user role is not Staff (0).';
  END IF;
END$$

-- ─────────────────────────────────────────
-- TRIGGER 3: validate role before inserting into volunteers
-- Only users with role=1 may have a row in the volunteers table
-- ─────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_volunteers_check_role$$

CREATE TRIGGER trg_volunteers_check_role
BEFORE INSERT ON volunteers
FOR EACH ROW
BEGIN
  DECLARE user_role INT;
  SELECT role INTO user_role FROM users WHERE user_id = NEW.user_id;

  IF user_role IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot insert into volunteers: user does not exist.';
  END IF;

  IF user_role != 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot insert into volunteers: user role is not Volunteer (1).';
  END IF;
END$$

-- ─────────────────────────────────────────
-- TRIGGER 4: prevent role from being changed on users
-- if it would orphan or conflict with subtype rows
-- ─────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_users_prevent_role_change$$

CREATE TRIGGER trg_users_prevent_role_change
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
  IF NEW.role != OLD.role THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot change user role directly. Delete the subtype row first, then update the role.';
  END IF;
END$$

-- ─────────────────────────────────────────
-- TRIGGER 5: trg_after_distribution_items_insert
-- Decrements inventory batch quantity when a distribution item is inserted.
-- ─────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_after_distribution_items_insert$$

CREATE TRIGGER trg_after_distribution_items_insert
AFTER INSERT ON distribution_items
FOR EACH ROW
BEGIN
  -- Check if decrement would bring inventory below 0
  IF (SELECT quantity FROM inventories WHERE inventory_id = NEW.inventory_id) < NEW.quantity THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Insufficient stock: quantity would go below 0.';
  END IF;

  -- Decrement inventory
  UPDATE inventories
  SET quantity = quantity - NEW.quantity
  WHERE inventory_id = NEW.inventory_id;

  -- Log the decrement
  INSERT INTO trigger_logs (trigger_name, message)
  VALUES ('trg_after_distribution_items_insert',
    CONCAT('Decremented inventory_id: ', NEW.inventory_id, ' by ', NEW.quantity));
END$$

-- ─────────────────────────────────────────
-- TRIGGER 6: trg_volunteer_shift_no_overlap_insert
-- Prevent volunteer double-booking on insert
-- ─────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_volunteer_shift_no_overlap_insert$$

CREATE TRIGGER trg_volunteer_shift_no_overlap_insert
BEFORE INSERT ON volunteer_shifts
FOR EACH ROW
BEGIN
  DECLARE overlap_count INT;

  SELECT COUNT(*) INTO overlap_count
  FROM volunteer_shifts
  WHERE volunteer_id      = NEW.volunteer_id
    AND shift_date        = NEW.shift_date
    AND shift_time_start  < NEW.shift_time_end
    AND shift_time_end    > NEW.shift_time_start
    AND NOT (shift_time_end = NEW.shift_time_start OR shift_time_start = NEW.shift_time_end);

  IF overlap_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Volunteer already has an overlapping shift on this date.';
  END IF;
END$$

-- ────────────────────
-- TRIGGER 7: trg_volunteer_shift_no_overlap_update
-- Prevent volunteer double-booking on update
-- ─────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_volunteer_shift_no_overlap_update$$

CREATE TRIGGER trg_volunteer_shift_no_overlap_update
BEFORE UPDATE ON volunteer_shifts
FOR EACH ROW
BEGIN
  DECLARE overlap_count INT;

  SELECT COUNT(*) INTO overlap_count
  FROM volunteer_shifts
  WHERE volunteer_id      = NEW.volunteer_id
    AND shift_date        = NEW.shift_date
    AND shift_time_start  < NEW.shift_time_end
    AND shift_time_end    > NEW.shift_time_start
    AND NOT (shift_time_end = NEW.shift_time_start OR shift_time_start = NEW.shift_time_end)
    AND shift_id         != NEW.shift_id;  -- exclude the row being updated

  IF overlap_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Volunteer already has an overlapping shift on this date.';
  END IF;
END$$

DELIMITER ;
