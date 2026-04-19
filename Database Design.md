# Database Design.md

---

## Project Overview

PrimaryFeed is a food bank management system built by **The Primary Keys** (Group 6 — CS 5200). The system's core value is its centralized MySQL relational database — the UI and API are intentionally thin shells that expose and demonstrate the schema's power.

### Problem context

Food banks commonly face five operational challenges that PrimaryFeed is designed to address:

1. **Inventory mismanagement** — difficulty tracking quantities, expiry dates, and storage conditions across branches
2. **Food waste** — expired food due to lack of timely expiry tracking
3. **Volunteer coordination issues** — manual scheduling causes double-booking or understaffing
4. **Data fragmentation** — donor, volunteer, and distribution data siloed with no unified view
5. **Lack of reporting** — insufficient data limits analysis of demand, donation trends, and volunteer activity

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React (SPA) |
| Backend | Java Spring Boot |
| ORM | Spring Data JPA |
| Database | MySQL |
| Auth | JWT (Spring Security) |
| Deployment | GCP Compute Engine (local for development and testing) |

---

## Roles

There are exactly two user roles. Role is stored as a `TINYINT` on the `users` table (`users.role`) and encoded in the JWT.

| Role | `users.role` value | DB subtype | Permissions |
|---|---|---|---|
| `staff` | `0` | `staff` table | Full CRUD + reports page |
| `volunteer` | `1` | `volunteers` table | CRUD only; no reports access (403) |

> **Design note:** Phase 1 originally specified three roles (Admin, Volunteer, Staff). For MVP simplicity, Admin has been merged into `staff`. The `admin_permissions` and `user_admin_permissions` tables exist and grant fine-grained permissions to any user via `user_id`, but no UI is built for them in the MVP.

Beneficiaries and donors do not have system accounts and cannot log in. Their records are managed by staff on their behalf. This is a deliberate MVP simplification — beneficiaries and donors are expected to interact with the food bank in person or by phone.

---

## Database

The database is named **primaryfeed**. It is the star of the system — most project value lives here.

### Schema entry point

The DDL is in `dbDDL.sql`. It sources two additional files at the end:
- `dbTRIGGERS.sql` — trigger definitions (results logged to `trigger_logs`)
- `dbPROCS.sql` — stored procedure definitions

### Tables

**Core entities**

| Table | Notes |
|---|---|
| `addresses` | Shared lookup table. No unique constraint — deduplication handled at app layer. |
| `food_banks` | Top-level organizations. `email` is unique. |
| `food_bank_branches` | Branches per food bank. Surrogate PK `branch_id`; business key `(branch_num, food_bank_id)` is unique. |
| `users` | Supertype for all users. `role TINYINT` (`0=Staff, 1=Volunteer`), `status TINYINT` (`0=Inactive, 1=Active`). Each user belongs to one `branch_id`. |
| `staff` | Subtype of `users`. Extra fields: `job_title`, `hire_date`. FK to `users.user_id` (unique). |
| `volunteers` | Subtype of `users`. Extra fields: `availability`, `background_check`. FK to `users.user_id` (unique). |
| `admin_permissions` | Catalog of permission types available to staff. Schema-only in MVP. |
| `user_admin_permissions` | Junction table granting permissions to any user (staff or volunteer) via `user_id` FK to `users.user_id`. Schema-only in MVP. |
| `food_categories` | Categories for food items. |
| `food_items` | Food catalog. Primary key is `sku VARCHAR(45)` — not an integer. `storage_condition` records required handling (e.g. "refrigerated") as a manual guide for staff — no automated inspection tracking in the MVP. |

**Operational events**

| Table | Notes |
|---|---|
| `inventories` | Stock per branch/item/expiry. The same `food_sku` may exist at multiple branches simultaneously. Decremented by distributions. |
| `volunteer_shifts` | Shift scheduling. Unique on `(volunteer_id, branch_id, shift_date, shift_time_start)`. FK to `volunteers.volunteer_id`. Overlap across different branches is prevented by triggers (see Triggers section). |
| `donors` | Individuals or organizations. `donor_type TINYINT` (`0=Individual, 1=Organization`). No login access. |
| `beneficiaries` | Recipients. `eligibility_status TINYINT` (`0=Ineligible, 1=Eligible`). No login access. |
| `donations` | Header record. FKs to `food_bank_branches`, `donors`, and `users`. |
| `donation_items` | Line items per donation. Includes `unit` and `expiry_date`. Unique on `(donation_id, food_sku, expiry_date)` — same SKU may appear twice with different expiry dates. |
| `distributions` | Header record. FKs to `food_bank_branches`, `beneficiaries`, and `users`. |
| `distribution_items` | Line items per distribution. References `inventory_id` directly (not `food_sku`). Unique on `(distribution_id, inventory_id)`. |

**System**

| Table | Notes |
|---|---|
| `trigger_logs` | Append-only log written to by DB triggers. `created_at` defaults to `NOW()`. |

### Views

| View | Purpose |
|---|---|
| `vw_expiring_inventory` | Items with `quantity > 0` within their expiry threshold, with `days_until_expiry`, `expiry_threshold_days`, and `perishability_tier` computed. Uses a tiered alert system based on `food_categories.category_name`: perishables (Produce, Dairy, Meat, Seafood, Bakery) use a 7-day window; all other categories use a 90-day window. |
| `vw_volunteer_hours_log_last_30_days` | Shift records from the last 30 days. Joins `volunteer_shifts` → `volunteers` → `users` for names, with `total_hours` formatted as `Xh Ym`. Use this for Query 7. |

### Triggers

`dbTRIGGERS.sql` includes the following triggers:

**`trg_volunteer_shift_no_overlap_insert` / `trg_volunteer_shift_no_overlap_update`**
Prevent a volunteer from being booked into overlapping shifts across any branch on the same date. Back-to-back shifts where a previous shift's end time equals the next shift's start time are explicitly permitted, reflecting real-world volunteer scheduling practice. Raises `SQLSTATE '45000'` with a descriptive message on violation. The `UPDATE` trigger excludes the row being updated via `AND shift_id != NEW.shift_id` to avoid false positives on edits.

### Key constraints to remember

- `food_items` PK is `sku VARCHAR(45)` — never treat it as an integer
- `users.role` determines the application role — always read this when issuing the JWT
- `distribution_items` links to `inventories.inventory_id`, not to `food_sku` directly
- `donation_items` unique key includes `expiry_date` — the same SKU can appear twice in one donation with different expiry dates
- `donations` and `distributions` both require a `user_id` FK referencing `users.user_id` — either a staff member or volunteer must be associated with every transaction
- `user_admin_permissions` references `users.user_id` directly — permissions can be granted to any user regardless of role
- All food intake and distribution actions must be performed by a registered user (staff or volunteer) — no anonymous transactions

---

## Key DB Assumptions

1. Each user has exactly one role (`staff` or `volunteer`). Phase 1 included Admin as a third role; merged into `staff` for MVP simplicity.
2. Phase 1 assumed a separate `Item_Received_ID` per food item. This evolved into the `inventories`, `donation_items`, and `distribution_items` tables to more accurately represent multi-branch stock and batch-level tracking.
3. The same `food_sku` may exist across multiple branches simultaneously in `inventories` — Phase 1's assumption that each food item lives at exactly one branch was revised as a design error.
4. All perishable items must have an expiry date recorded.
5. Volunteers may not have overlapping shifts across any branch. Enforced at the DB level via `BEFORE INSERT` and `BEFORE UPDATE` triggers in `dbTRIGGERS.sql`.
6. Every distribution must reference a valid inventory record, branch, and beneficiary.
7. Foreign key constraints are enforced throughout.
8. All food intake and distribution actions must be performed by a registered user (staff or volunteer). The `user_id` FK on `donations` and `distributions` references `users.user_id` directly, allowing either role to record transactions.
9. Historical records are retained for auditing and reporting.

---

## API Design

REST over HTTPS. JWT passed in the `Authorization: Bearer <token>` header.

### Endpoint groups

| Group | Routes | Access |
|---|---|---|
| Auth | `POST /auth/login` | Public |
| Inventory | `/inventory/**` | All |
| Donations | `/donations/**`, `/donation-items/**` | All |
| Distributions | `/distributions/**`, `/distribution-items/**` | All |
| Beneficiaries | `/beneficiaries/**` | All |
| Donors | `/donors/**` | All |
| Volunteers | `/volunteers/**`, `/shifts/**` | All |
| Reports | `/reports/**` | Staff only (403 for volunteers) |

---

## Frontend Pages

| Route | Page | Access |
|---|---|---|
| `/login` | Login | Public |
| `/dashboard` | Dashboard — metric cards | Staff only |
| `/inventory` | Inventory view with expiry flags | All |
| `/donations` | Log and view donations | All |
| `/distributions` | Log and view distributions | All |
| `/reports` | Pre-built insight query results | Staff only |

---

## Insight Queries

All 17 queries from the original project specification. Do not rewrite them unless explicitly asked.

1. All food items currently available at a specific branch
2. Food items expiring within the next 3 days
3. Total food quantity available across all branches
4. Food items grouped by category
5. Branch that distributed the most food last month (uses CTE)
6. All volunteers assigned to a specific branch
7. Volunteer hours per volunteer in the last 30 days — use `vw_volunteer_hours_log_last_30_days`
8. Volunteers at a specific branch during a given time window on a given date
9. Distribution history for a specific beneficiary
10. Number of beneficiaries served per branch this week
11. All donations received from a specific donor
12. Total food received vs. distributed per branch (net surplus)
13. Food items below a user-supplied quantity threshold (threshold passed as a query parameter at runtime — no threshold table)
14. All users and their assigned roles
15. Food categories most frequently donated or distributed
16. Daily distribution totals for the last 14 days — cast `distribution_date` to `DATE` for grouping
17. Branches with the highest volunteer-to-distribution ratio

---

## Coding Conventions

- Follow standard Spring Boot project structure: `controller`, `service`, `repository`, `model` packages
- Entity classes map 1:1 to DB tables; use `@Entity`, `@Table`, `@Id`, `@Column` annotations
- `food_items` PK maps to a `String` field in Java, not `Long` or `Integer`
- `food_categories` must be treated as a controlled reference table — the application must not allow users to freely add, edit, or delete categories. The perishability tier logic in `vw_expiring_inventory` depends on exact category name matches (`Produce`, `Dairy`, `Meat`, `Seafood`, `Bakery`). Any category name change will silently break the tiered expiry logic. Category management, if needed, should be a privileged operation gated behind a future admin UI with strict validation.
- Use `@PreAuthorize("hasRole('STAFF')")` or a `SecurityConfig` filter chain to gate the reports routes
- Insight queries belong in the `service` layer as `@Query` methods or native SQL via `EntityManager` — not inlined in controllers
- Frontend API calls go through a single `api.js` utility that attaches the JWT header automatically
- Role enum in Java: `STAFF(0), VOLUNTEER(1)` — match the `users.role TINYINT` values exactly
- Trigger violations (`SQLSTATE '45000'`) surface as Spring `DataIntegrityViolationException` — parse `MESSAGE_TEXT` to return a clean error to the frontend

---

## Out of Scope (MVP)

Do not implement the following unless explicitly asked:

- User self-registration or password reset flows
- Permission management UI (`admin_permissions` / `user_admin_permissions` are schema-only)
- Beneficiary or donor login
- Food inspection status or physical storage location tracking
- Pagination beyond a reasonable row cap
- File uploads, email notifications
