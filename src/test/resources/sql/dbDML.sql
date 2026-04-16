-- PrimaryFeed DML
-- Sample data for all tables (7-10 rows each)
-- Includes some UPDATE and DELETE statements for realism
-- NOTE: addresses must be inserted first since other tables reference address_id

USE primaryfeed;

-- ─────────────────────────────────────────
-- ADDRESSES
-- ─────────────────────────────────────────
INSERT INTO addresses (address_line_1, address_line_2, city, state, zip_code) VALUES
  ('100 Federal St',     NULL,       'Boston',   'MA', '02110'),  -- 1: BAFB HQ
  ('250 Albany St',      NULL,       'Boston',   'MA', '02118'),  -- 2: Downtown Boston branch
  ('300 Tremont St',     NULL,       'Boston',   'MA', '02116'),  -- 3: South End branch
  ('460 Blue Hill Ave',  NULL,       'Boston',   'MA', '02121'),  -- 4: Roxbury branch
  ('45 Market St',       NULL,       'Lynn',     'MA', '01901'),  -- 5: GLFP HQ & Lynn Central (shared)
  ('120 Boston St',      NULL,       'Lynn',     'MA', '01905'),  -- 6: Lynn North branch
  ('10 Maple Ave',       'Apt 3B',   'Boston',   'MA', '02119'),  -- 7: shared by two users
  ('88 Beacon St',       NULL,       'Boston',   'MA', '02108'),  -- 8: user address
  ('22 River Rd',        'Unit 1',   'Lynn',     'MA', '01902'),  -- 9: user address
  ('5 Oak St',           NULL,       'Boston',   'MA', '02130'),  -- 10: donor address
  ('300 Boylston St',    NULL,       'Boston',   'MA', '02116'),  -- 11: donor/org address
  ('77 Salem St',        'Floor 2',  'Boston',   'MA', '02113'),  -- 12: beneficiary address
  ('14 Elm St',          NULL,       'Lynn',     'MA', '01903'),  -- 13: beneficiary address
  ('90 Cambridge St',    NULL,       'Boston',   'MA', '02114'),  -- 14: beneficiary address
  ('33 Dorchester Ave',  NULL,       'Boston',   'MA', '02127');  -- 15: Dorchester branch

-- ─────────────────────────────────────────
-- FOOD_BANKS
-- ─────────────────────────────────────────
INSERT INTO food_banks (name, email, phone, address_id) VALUES
  ('Boston Area Food Bank',    'contact@bafb.org', '6175550101', 1),
  ('Greater Lynn Food Pantry', 'info@glfp.org',    '7815550202', 5);

-- ─────────────────────────────────────────
-- FOOD_BANK_BRANCHES
-- branch_num restarts from 1 per food bank, illustrating the weak entity relationship
-- ─────────────────────────────────────────
INSERT INTO food_bank_branches (branch_num, branch_name, food_bank_id, address_id) VALUES
  (1, 'Downtown Boston', 1, 2),
  (2, 'South End',       1, 3),
  (3, 'Roxbury',         1, 4),
  (4, 'Dorchester',      1, 15),
  (5, 'Jamaica Plain',   1, NULL),  -- address not yet registered
  (1, 'Lynn Central',    2, 5),     -- shares address with GLFP HQ
  (2, 'Lynn North',      2, 6),
  (3, 'Lynn Harbor',     2, NULL),  -- address not yet registered
  (4, 'Lynn Woods',      2, NULL),  -- address not yet registered
  (5, 'Swampscott',      2, NULL);  -- address not yet registered

-- ─────────────────────────────────────────
-- USERS
-- role: 0=Staff, 1=Volunteer | status: 0=Inactive, 1=Active
-- ─────────────────────────────────────────
INSERT INTO users (first_name, last_name, email, phone, password_hash, role, status, branch_id, address_id, driving_license_num) VALUES
  ('Alice',  'Nguyen', 'alice.nguyen@bafb.org',  '6175551001', 'hashed_pw_1',  0, 1, 1, 7,    'MA-001-ALICE'),
  ('Bob',    'Smith',  'bob.smith@bafb.org',     '6175551002', 'hashed_pw_2',  0, 1, 1, 8,    NULL),
  ('Carol',  'Lee',    'carol.lee@bafb.org',     '6175551003', 'hashed_pw_3',  1, 1, 2, 7,    'MA-003-CAROL'), -- shares address with Alice
  ('David',  'Kim',    'david.kim@bafb.org',     '7815551004', 'hashed_pw_4',  0, 1, 3, NULL, NULL),
  ('Eva',    'Patel',  'eva.patel@bafb.org',     '7815551005', 'hashed_pw_5',  1, 1, 3, NULL, 'MA-005-EVAP'),
  ('Frank',  'Ortiz',  'frank.ortiz@glfp.org',   '6175551006', 'hashed_pw_6',  0, 1, 6, 9,    NULL),
  ('Grace',  'Wang',   'grace.wang@glfp.org',    '6175551007', 'hashed_pw_7',  1, 1, 6, 9,    'MA-007-GRACE'), -- shares address with Frank
  ('Henry',  'Brown',  'henry.brown@glfp.org',   '6175551008', 'hashed_pw_8',  0, 1, 7, NULL, NULL),
  ('Iris',   'Chen',   'iris.chen@glfp.org',     '6175551009', 'hashed_pw_9',  1, 0, 7, NULL, 'MA-009-IRIS'),
  ('James',  'Taylor', 'james.taylor@glfp.org',  '7815551010', 'hashed_pw_10', 0, 1, 7, NULL, NULL);

-- ─────────────────────────────────────────
-- VOLUNTEERS
-- ─────────────────────────────────────────
INSERT INTO volunteers (user_id, availability, background_check) VALUES
  (3,  'Weekends',         1),
  (5,  'Weekday mornings', 1),
  (7,  'Flexible',         1),
  (9,  'Saturdays only',   0);

-- ─────────────────────────────────────────
-- STAFF
-- ─────────────────────────────────────────
INSERT INTO staff (user_id, job_title, hire_date) VALUES
  (1,  'Branch Manager',        '2021-03-15 09:00:00'),
  (2,  'Inventory Coordinator', '2022-07-01 09:00:00'),
  (4,  'Distribution Manager',  '2023-01-10 09:00:00'),
  (6,  'Operations Lead',       '2020-11-20 09:00:00'),
  (8,  'Logistics Coordinator', '2022-05-05 09:00:00'),
  (10, 'Branch Manager',        '2021-06-01 09:00:00');

-- ─────────────────────────────────────────
-- ADMIN_PERMISSIONS
-- ─────────────────────────────────────────
INSERT INTO admin_permissions (permission_type, permission_description) VALUES
  (0, 'Create, update, and deactivate user accounts'),
  (1, 'Add, update, and remove inventory records'),
  (2, 'View analytics, inventory, and operational reports'),
  (3, 'Record and manage incoming food donations'),
  (4, 'Record and manage outgoing food distributions');

-- ─────────────────────────────────────────
-- USER_ADMIN_PERMISSIONS
-- ─────────────────────────────────────────
INSERT INTO user_admin_permissions (user_id, permission_id) VALUES
  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
  (2, 2), (2, 4), (2, 5),
  (3, 4), (3, 5),
  (4, 1), (4, 2), (4, 4),
  (5, 4), (5, 5),
  (6, 2), (6, 3),
  (7, 4), (7, 5),
  (8, 2), (8, 4),
  (10, 1), (10, 2), (10, 3), (10, 4), (10, 5);

-- ─────────────────────────────────────────
-- FOOD_CATEGORIES
-- ─────────────────────────────────────────
INSERT INTO food_categories (category_name, category_description) VALUES
  ('Canned Goods',  'Canned vegetables, soups, and proteins'),
  ('Dairy',         'Milk, cheese, yogurt, and eggs'),
  ('Produce',       'Fresh fruits and vegetables'),
  ('Grains',        'Rice, pasta, bread, and cereals'),
  ('Protein',       'Meat, poultry, fish, and legumes'),
  ('Beverages',     'Juice, water, and non-alcoholic drinks'),
  ('Condiments',    'Sauces, oils, spices, and spreads'),
  ('Frozen Foods',  'Frozen meals, vegetables, and proteins'),
  ('Snacks',        'Crackers, granola bars, and dried fruit'),
  ('Baby Food',     'Formula, purees, and baby snacks');

-- ─────────────────────────────────────────
-- FOOD_ITEMS
-- ─────────────────────────────────────────
INSERT INTO food_items (sku, food_name, food_description, storage_condition, category_id) VALUES
  ('SKU-001', 'Canned Chickpeas',    'Cooked chickpeas in water',        'Cool dry place',   1),
  ('SKU-011', 'Canned Tomatoes',     'Diced tomatoes in juice',          'Cool dry place',   1), -- second Canned Good
  ('SKU-002', 'Whole Milk (1L)',     'Pasteurized whole milk',           'Refrigerated',     2),
  ('SKU-003', 'Russet Potatoes',    '5lb bag of russet potatoes',       'Cool dry place',   3),
  ('SKU-004', 'White Rice (5lb)',   'Long grain white rice',            'Cool dry place',   4),
  ('SKU-005', 'Chicken Breast',     'Boneless skinless chicken breast', 'Frozen',           5),
  ('SKU-006', 'Apple Juice (1L)',   '100% apple juice',                 'Room temperature', 6),
  ('SKU-007', 'Olive Oil (500ml)',  'Extra virgin olive oil',           'Cool dark place',  7),
  ('SKU-008', 'Frozen Mixed Veg',  'Peas, carrots, corn blend',        'Frozen',           8),
  ('SKU-009', 'Granola Bars (box)','Oats and honey granola bars',      'Room temperature', 9),
  ('SKU-010', 'Baby Formula (900g)','Infant formula stage 1',          'Cool dry place',  10);

-- ─────────────────────────────────────────
-- VOLUNTEER_SHIFTS
-- ─────────────────────────────────────────
INSERT INTO volunteer_shifts (volunteer_id, branch_id, shift_date, shift_time_start, shift_time_end, shift_notes) VALUES
  (1, 1, '2026-04-05', '09:00:00', '12:00:00', 'Sorting donated canned goods'),
  (1, 1, '2026-04-12', '09:00:00', '12:00:00', 'Inventory count'),
  (2, 3, '2026-04-05', '08:00:00', '11:00:00', 'Morning distribution prep'),
  (2, 3, '2026-04-07', '08:00:00', '11:00:00', NULL),
  (3, 6, '2026-04-06', '10:00:00', '14:00:00', 'Food drive support'),
  (3, 6, '2026-04-13', '10:00:00', '14:00:00', NULL),
  (4, 7, '2026-04-05', '09:00:00', '13:00:00', 'Pending background check — limited duties'),
  (1, 2, '2026-04-19', '09:00:00', '12:00:00', 'Cross-branch support shift'),
  (2, 4, '2026-04-10', '13:00:00', '17:00:00', 'Afternoon donation intake');

-- VOLUNTEER_SHIFTS — trigger test cases
-- Valid: volunteer 1 back-to-back on same branch same day as existing 09:00–12:00 shift
INSERT INTO volunteer_shifts (volunteer_id, branch_id, shift_date, shift_time_start, shift_time_end, shift_notes) VALUES
  (1, 1, '2026-04-05', '12:00:00', '15:00:00', 'Back-to-back with morning sorting shift');

-- Should fail: volunteer 1 overlaps with existing 09:00–12:00 shift on 2026-04-1
INSERT INTO volunteer_shifts (volunteer_id, branch_id, shift_date, shift_time_start, shift_time_end, shift_notes) VALUES
  (1, 2, '2026-04-12', '11:00:00', '14:00:00', 'Overlaps with inventory count shift at branch 1');

-- ─────────────────────────────────────────
-- DONORS
-- ─────────────────────────────────────────
INSERT INTO donors (donor_name, donor_type, email, phone, address_id) VALUES
  ('John Carter',           0, 'john.carter@email.com',    '6175552001', 10),
  ('Whole Foods Market',    1, 'donations@wholefoods.com', '6175552002', 11),
  ('Maria Gonzalez',        0, 'maria.g@email.com',        '7815552003', NULL),
  ('Stop & Shop',           1, 'giving@stopshop.com',      '6175552004', NULL),
  ('Priya Sharma',          0, 'priya.s@email.com',        '6175552005', NULL),
  ('Greater Boston Church', 1, 'outreach@gbchurch.org',    '6175552006', 11), -- shares address with Whole Foods building
  ('Tom Nguyen',            0, 'tom.n@email.com',          '7815552007', NULL),
  ('Trader Joe''s',         1, 'community@traderjoes.com', '6175552008', NULL);

-- ─────────────────────────────────────────
-- BENEFICIARIES
-- ─────────────────────────────────────────
-- eligibility_status: 0=Ineligible, 1=Eligible
INSERT INTO beneficiaries (beneficiary_full_name, household_size, phone, email, eligibility_status, address_id) VALUES
  ('Linda Park',      3, '6175553001', 'linda.park@email.com', 1, 12),
  ('Marcus Johnson',  5, '6175553002', NULL,                   1, NULL),
  ('Sofia Reyes',     2, '7815553003', 'sofia.r@email.com',    1, 13),
  ('Ahmed Hassan',    4, '6175553004', 'ahmed.h@email.com',    1, 14),
  ('Wei Zhang',       6, '6175553005', NULL,                   1, 12), -- shares address with Linda Park
  ('Nina Kowalski',   1, '7815553006', 'nina.k@email.com',     0, NULL),
  ('Derek Thompson',  3, '6175553007', 'derek.t@email.com',    1, NULL),
  ('Fatima Ali',      7, '6175553008', 'fatima.a@email.com',   1, NULL),
  ('Carlos Mendez',   2, '7815553009', NULL,                   1, 13), -- shares address with Sofia Reyes
  ('Ruth Okafor',     4, '6175553010', 'ruth.o@email.com',     1, NULL);

-- Mark ineligible beneficiary as eligible after re-evaluation
UPDATE beneficiaries SET eligibility_status = 1 WHERE beneficiary_id = 6;

-- ─────────────────────────────────────────
-- INVENTORIES
-- Same SKU can appear multiple times per branch with different expiry dates (different batches)
-- ─────────────────────────────────────────
INSERT INTO inventories (food_sku, branch_id, quantity, unit, expiry_date) VALUES
  ('SKU-001', 1, 0, 'cans', DATE_ADD(CURDATE(), INTERVAL 45 DAY)),
  ('SKU-001', 1, 0, 'cans', '2026-12-31 00:00:00');

-- Reduce Baby Formula quantity to simulate a manual stock correction
UPDATE inventories SET quantity = 12 WHERE food_sku = 'SKU-010' AND branch_id = 9;

-- ─────────────────────────────────────────
-- DONATIONS
-- ─────────────────────────────────────────
INSERT INTO donations (branch_id, donor_id, user_id, donation_date) VALUES
  (1, 1, 1, '2026-03-01 10:00:00'),
  (1, 2, 1, '2026-03-05 14:00:00'),
  (3, 3, 4, '2026-03-10 09:30:00'),
  (3, 4, 4, '2026-03-12 11:00:00'),
  (5, 5, 4, '2026-03-15 13:00:00'),
  (6, 6, 8, '2026-03-18 10:00:00'),
  (7, 7, 8, '2026-03-20 15:00:00'),
  (8, 8, 8, '2026-03-22 09:00:00'),
  (4, 1, 1, '2026-03-24 10:00:00'),
  (9, 2, 8, '2026-03-25 10:00:00');

-- ─────────────────────────────────────────
-- DONATION_ITEMS
-- ─────────────────────────────────────────
INSERT INTO donation_items (donation_id, food_sku, quantity, unit, expiry_date) VALUES
  -- donation 1 at branch 1: increments pre-seeded SKU-001 batches
  (1, 'SKU-001', 120, 'cans', '2026-12-31 00:00:00'),
  (1, 'SKU-001', 38, 'cans', DATE_ADD(CURDATE(), INTERVAL 45 DAY)),
  (1, 'SKU-011', 60, 'cans', DATE_ADD(CURDATE(), INTERVAL 60 DAY)),
  -- donation 2 at branch 1: creates SKU-002
  (2, 'SKU-002', 30, 'liters', DATE_ADD(CURDATE(), INTERVAL 3 DAY)),
  -- donation 3 at branch 3: creates SKU-003 at branch 3
  (3, 'SKU-003', 25, 'bags', DATE_ADD(CURDATE(), INTERVAL 5 DAY)),
  -- donation 4 at branch 3: creates SKU-003 at branch 3 different expiry, SKU-004
  (4, 'SKU-003', 50, 'bags', '2026-05-01 00:00:00'),
  (4, 'SKU-004', 200, 'bags', '2027-06-30 00:00:00'),
  -- donation 5 at branch 5: creates SKU-006
  (5, 'SKU-006', 60, 'liters', '2026-08-20 00:00:00'),
  -- donation 6 at branch 6: creates SKU-007
  (6, 'SKU-007', 45, 'bottles', '2027-01-01 00:00:00'),
  -- donation 7 at branch 7: creates SKU-008
  (7, 'SKU-008', 100, 'bags', '2027-03-01 00:00:00'),
  -- donation 8 at branch 8: creates SKU-009
  (8, 'SKU-009', 75, 'boxes', DATE_ADD(CURDATE(), INTERVAL 75 DAY)),
  -- donation 9 at branch 4: creates SKU-005 two batches
  (9, 'SKU-005',  80, 'lbs', DATE_ADD(CURDATE(), INTERVAL 4 DAY)), 
  (9, 'SKU-005',  30, 'lbs', '2026-06-01 00:00:00'),
  -- donation 10 at branch 9: creates SKU-010
  (10, 'SKU-010', 12, 'tins', '2026-07-15 00:00:00');

-- ─────────────────────────────────────────
-- DISTRIBUTIONS
-- ─────────────────────────────────────────
INSERT INTO distributions (branch_id, beneficiary_id, user_id, distribution_date) VALUES
  (1, 1,  1, '2026-03-03 11:00:00'),
  (1, 2,  1, '2026-03-03 11:30:00'),
  (3, 3,  4, '2026-03-11 10:00:00'),
  (3, 4,  4, '2026-03-11 10:30:00'),
  (5, 5,  4, '2026-03-16 13:00:00'),
  (6, 7,  6, '2026-03-19 09:00:00'),
  (7, 8,  8, '2026-03-21 14:00:00'),
  (8, 9,  8, '2026-03-23 10:00:00'),
  (1, 10, 1, '2026-03-25 11:00:00');

-- ─────────────────────────────────────────
-- DISTRIBUTION_ITEMS
-- ─────────────────────────────────────────
INSERT INTO distribution_items (distribution_id, quantity, inventory_id) VALUES
  (1, 5,  1),
  (1, 2,  4),
  (2, 3,  3),
  (3, 2,  2),
  (3, 4,  12),
  (4, 3,  7),
  (5, 5,  9),
  (6, 2,  10),
  (7, 6,  11),
  (8, 1,  13),
  (9, 4,  1),
  (9, 3,  4),
  (9, 3,  14); -- SKU-011 distributed in distribution 9

-- ─────────────────────────────────────────
-- CLEANUP EXAMPLE (DELETE)
-- Remove a volunteer shift logged in error for an uncleared volunteer
-- ─────────────────────────────────────────
DELETE FROM volunteer_shifts
WHERE volunteer_id = 4
  AND shift_date = '2026-04-05'
  AND shift_notes LIKE '%Pending background check%';
