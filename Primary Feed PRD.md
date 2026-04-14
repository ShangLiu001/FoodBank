# PrimaryFeed — MVP Product Requirements Document

**Project:** PrimaryFeed Food Bank Management System
**Team:** The Primary Keys (Group 6 — CS 5200)
**Version:** 1.2 (MVP)

---

## 1. Overview

PrimaryFeed is a food bank management system built around a centralized MySQL relational database. The database is the core value of the system — the UI and API are intentionally thin, designed to expose and demonstrate the power of the underlying schema. The MVP covers the minimum surface area needed to demonstrate authenticated access, daily CRUD operations, and data-driven insights.

---

## 2. Problem Statement

Food banks operate by collecting food donations, storing them safely, and distributing them to individuals or partner organizations through volunteers and staff across multiple branches. Without a centralized system, food banks commonly face the following operational challenges:

- **Inventory mismanagement** — difficulty tracking food quantities, expiration dates, and storage conditions across branches
- **Food waste** — expired food due to lack of timely expiry tracking and distribution prioritization
- **Volunteer coordination issues** — manual scheduling leads to understaffing, overlap, or double-booking
- **Data fragmentation** — donor, volunteer, and distribution data stored separately with no unified view
- **Lack of reporting** — insufficient recorded data limits the ability to analyze demand, donation trends, and volunteer activity

PrimaryFeed addresses these challenges by centralizing all operational data in a single relational database, enabling staff and volunteers to act on accurate, real-time information.

---

## 3. Real-World Relevance

Food insecurity affects millions of people worldwide. A reliable Food Bank Management System helps reduce food waste, improves transparency, and ensures that donated food reaches those in need efficiently. This system reflects real-world operational challenges faced by non-profit organizations and demonstrates the practical application of relational database design.

---

## 4. Target Users

| User type | Access | Description |
|---|---|---|
| Staff | System login | Manages branches, inventory, donations, distributions, volunteers, and reports. Every food intake and distribution transaction must be associated with a registered user (staff or volunteer). |
| Volunteer | System login | Views schedules and assigned branch. Records food intake and distribution under staff supervision. |
| Beneficiary | No system login | Receives food assistance. Records managed by staff on their behalf. Beneficiaries are encouraged to visit a branch in person or call to allow staff to verify their identity. |
| Donor | No system login | Individuals or organizations donating food. Donor records managed by staff. Donors interact with the food bank in person or by phone. |

---

## 5. Users & Roles

| Role | `users.role` value | DB subtype | Permissions |
|---|---|---|---|
| `staff` | `0` | `staff` table | Full CRUD + reports page |
| `volunteer` | `1` | `volunteers` table | CRUD only; no reports access (403) |

> **Note:** Phase 1 originally specified three roles (Admin, Volunteer, Staff). For MVP simplicity, Admin has been merged into the `staff` role. The `admin_permissions` and `user_admin_permissions` tables exist in the schema to grant fine-grained permissions to users but are not surfaced in the MVP UI.

Both roles authenticate via the same login screen. Role is read from `users.role`, encoded in the issued JWT, and enforced at the API layer.

---

## 6. User Stories

**US-1 — Login**
As an authorized user (staff or volunteer), I want to log in with my credentials so that I can securely access the food bank management system.

**US-2 — Log a donation**
As a volunteer, I want to record an incoming donation (donor, items, quantities) so that inventory levels stay accurate and donors are tracked.

**US-3 — Process a distribution**
As a volunteer, I want to record food distributed to a beneficiary so that the system reflects what left the branch and who received it.

**US-4 — Check inventory & flag expiring items**
As a volunteer, I want to view current inventory for my branch and see items nearing expiry so that I can prioritize distributions before food goes to waste.

**US-5 — View operational insights**
As a staff member, I want to query the system for a summary of branch performance — including donation totals, distribution totals, surplus, and top donors — so that I can make informed decisions about resource allocation.

---

## 7. Scope

### In scope (MVP)
- JWT-based login and session management using `users.role`
- CRUD operations on: inventory, donations, donation items, distributions, distribution items, beneficiaries, donors, volunteers
- Inventory view backed by the `vw_expiring_inventory` view (items expiring within 3 months, with days remaining)
- Reports page with all 17 pre-built insight queries rendered as tables
- Role-based route protection (staff vs. volunteer)

### Out of scope (post-MVP)
- User self-registration or password reset
- Fine-grained permission management UI (`admin_permissions` / `user_admin_permissions` are schema-only in the MVP)
- Beneficiary and donor system login
- Food inspection status and physical storage location tracking (MVP records `storage_condition` on food items as a manual guide only)
- Pagination beyond a reasonable row limit
- File uploads, email notifications
- Adding, editing, or deleting food categories via the UI — `food_categories` is a controlled reference table. The tiered expiry logic in `vw_expiring_inventory` depends on exact category name matches; free editing of category names would silently break expiry alerts. Category management is a post-MVP privileged operation.

---

## 8. UI Pages

| Page | Accessible by | Purpose |
|---|---|---|
| Login | All | Credential entry, JWT issue |
| Dashboard | Staff only | Key metric cards at a glance |
| CRUD views | All | Tables, forms, and modals for daily operations |
| Reports | Staff only | Pre-built insight query results |

---

## 9. System Architecture

**Frontend:** React SPA — four pages, communicates with the backend over REST/HTTPS with JWT in the Authorization header.

**Backend:** Java Spring Boot — Auth service, CRUD controllers, Query service, Spring Data JPA as the ORM layer.

**Database:** MySQL — PrimaryFeed DB. The centralized schema is the primary deliverable of the project. Includes triggers (via `dbTRIGGERS.sql`), stored procedures (via `dbPROCS.sql`), and views.

**Deployment:** GCP Compute Engine. Local deployment used during development and testing.

---

## 10. Database — Tables & Views

### Tables

| Group | Tables |
|---|---|
| Core entities | `addresses`, `food_banks`, `food_bank_branches`, `users`, `staff`, `volunteers`, `admin_permissions`, `user_admin_permissions`, `food_categories`, `food_items` |
| Operational events | `inventories`, `volunteer_shifts`, `donors`, `beneficiaries`, `donations`, `donation_items`, `distributions`, `distribution_items` |
| System | `trigger_logs` |

### Views

| View | Purpose |
|---|---|
| `vw_expiring_inventory` | Items with `quantity > 0` within their expiry threshold, with `days_until_expiry`, `expiry_threshold_days`, and `perishability_tier` computed. Perishables (Produce, Dairy, Meat, Seafood, Bakery) use a 7-day alert window; all other categories use a 90-day window. |
| `vw_volunteer_hours_log_last_30_days` | Shift records from the last 30 days, joined through `volunteers` to `users` for names, with `total_hours` formatted as `Xh Ym` |

---

## 11. Insight Queries (Reports Page)

All 17 queries from the original project specification are supported:

1. Show all food items currently available at a specific branch
2. List all food items expiring within the next 3 days
3. Total food quantity available across all branches
4. Food items grouped by category
5. Branch that distributed the most food last month
6. All volunteers assigned to a specific branch
7. Volunteer hours worked per volunteer in the last 30 days (via `vw_volunteer_hours_log_last_30_days`)
8. Volunteers working at a specific branch during a given time window on a given date
9. Distribution history for a specific beneficiary
10. Number of beneficiaries served per branch this week
11. All food donations received from a specific donor
12. Total food received vs. distributed per branch (net surplus)
13. Food items with quantity below a user-supplied threshold (threshold provided as a parameter at query time)
14. All users and their assigned roles
15. Food categories most frequently donated or distributed
16. Daily food distribution totals for the last 14 days (`distribution_date` cast to `DATE` for grouping)
17. Branches with the highest volunteer-to-distribution ratio

---

## 12. Key DB Assumptions

1. Each user has exactly one role — `staff` or `volunteer`. (Phase 1 included a third Admin role; this has been merged into `staff` for MVP simplicity.)
2. Each user has a unique `user_id`. Each food item has a unique `sku`. Each branch has a unique `branch_id`.
3. The same `food_sku` may exist across multiple branches simultaneously in `inventories`. Individual received batches are represented via `donation_items` and `distribution_items`.
4. All perishable food items must have an expiration date recorded in `donation_items` and `inventories`.
5. Volunteers may be assigned to multiple branches over time but may not have overlapping shifts. Overlap is enforced at the DB level via `BEFORE INSERT` and `BEFORE UPDATE` triggers on `volunteer_shifts`.
6. Every distribution transaction must reference a valid inventory record, branch, and beneficiary.
7. Foreign key constraints are enforced throughout to ensure referential integrity.
8. All food intake and distribution actions must be performed by a registered user (staff or volunteer). The `user_id` FK on `donations` and `distributions` references `users.user_id` directly, allowing either role to record transactions.
9. Historical records are retained for auditing and reporting purposes.
