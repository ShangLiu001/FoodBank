---
paths:
  - "src/main/java/com/primaryfeed/entity/**"
  - "src/main/java/com/primaryfeed/repository/**"
  - "src/main/java/com/primaryfeed/dto/**"
  - "sql/**"
---

# PrimaryFeed Schema Reference

## Tables

### Core entities

| Table | Notes |
|---|---|
| `addresses` | Shared lookup table. No unique constraint — deduplication handled at app layer. |
| `food_banks` | Top-level organizations. `email` is unique. |
| `food_bank_branches` | Surrogate PK `branch_id`; business key `(branch_num, food_bank_id)` is unique. |
| `users` | Supertype for all users. `role TINYINT` (`0=Staff, 1=Volunteer`), `status TINYINT` (`0=Inactive, 1=Active`). Each user belongs to one `branch_id`. |
| `staff` | Subtype of `users`. Fields: `job_title`, `hire_date`. FK to `users.user_id` (unique). |
| `volunteers` | Subtype of `users`. Fields: `availability`, `background_check`. FK to `users.user_id` (unique). |
| `admin_permissions` | Catalog of permission types. Schema-only in MVP. |
| `user_admin_permissions` | Junction table granting permissions to any user via `user_id` FK. Schema-only in MVP. |
| `food_categories` | Categories for food items. Controlled reference table — names must match exactly for view logic. |
| `food_items` | PK is `sku VARCHAR(45)`. `storage_condition` is a manual guide for staff — no automated inspection. |

### Operational events

| Table | Notes |
|---|---|
| `inventories` | Stock per branch/item/expiry. Same `food_sku` may exist at multiple branches. Decremented by distributions. |
| `volunteer_shifts` | Unique on `(volunteer_id, branch_id, shift_date, shift_time_start)`. FK to `volunteers.volunteer_id`. Overlap prevented by triggers. |
| `donors` | `donor_type TINYINT` (`0=Individual, 1=Organization`). No login access. |
| `beneficiaries` | `eligibility_status TINYINT` (`0=Ineligible, 1=Eligible`). No login access. |
| `donations` | Header record. FKs to `food_bank_branches`, `donors`, and `users`. |
| `donation_items` | Line items. Includes `unit` and `expiry_date`. Unique on `(donation_id, food_sku, expiry_date)`. |
| `distributions` | Header record. FKs to `food_bank_branches`, `beneficiaries`, and `users`. |
| `distribution_items` | Line items. References `inventory_id` directly. Unique on `(distribution_id, inventory_id)`. |

### System

| Table | Notes |
|---|---|
| `trigger_logs` | Append-only log written by DB triggers. `created_at` defaults to `NOW()`. |

## Views

| View | Purpose |
|---|---|
| `vw_expiring_inventory` | Items with `quantity > 0` within expiry threshold. Perishables (Produce, Dairy, Protein, Frozen Foods, Baby Food) use 7-day window; others use 90-day. Computes `days_until_expiry`, `expiry_threshold_days`, `perishability_tier`. |
| `vw_volunteer_hours_log_last_30_days` | Shifts from last 30 days with `total_hours` as `Xh Ym`. Use for Query 7. |

## Triggers

**`trg_volunteer_shift_no_overlap_insert` / `trg_volunteer_shift_no_overlap_update`**
Prevent overlapping shifts across any branch on the same date. Back-to-back shifts (end = start) are permitted. Raises `SQLSTATE '45000'`. The UPDATE trigger excludes the current row via `shift_id != NEW.shift_id`.

## Key DB Assumptions

1. Each user has exactly one role (`staff` or `volunteer`).
2. `inventories`, `donation_items`, and `distribution_items` replaced Phase 1's `Item_Received_ID` for multi-branch batch tracking.
3. Same `food_sku` may exist across multiple branches simultaneously — Phase 1's one-branch-per-item assumption was a design error.
4. All perishable items must have an expiry date.
5. Volunteer shift overlap enforced by triggers.
6. Every distribution must reference a valid inventory record, branch, and beneficiary.
7. Foreign key constraints enforced throughout.
8. All transactions require a registered user — `user_id` FK on `donations` and `distributions` allows either role.
9. Historical records retained for auditing.
