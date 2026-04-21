# PrimaryFeed

Food bank management system (CS 5200, Group 6 — The Primary Keys). Centralized MySQL database with a thin Spring Boot API and React frontend.

## Tech Stack

React SPA → Spring Boot (JPA) → MySQL (`primaryfeed`). Auth via JWT (Spring Security). Deployed on GCP Compute Engine.

## Roles

Two roles stored as `users.role TINYINT`: `0=Staff` (full CRUD + reports), `1=Volunteer` (CRUD only, 403 on reports). Beneficiaries and donors have no login access.

Phase 1 originally had three roles (Admin, Volunteer, Staff). Admin was merged into `staff` for MVP. The `admin_permissions` and `user_admin_permissions` tables exist but have no UI in MVP.

## Key DB Constraints

- `food_items` PK is `sku VARCHAR(45)` — not an integer
- `donations.user_id` and `distributions.user_id` reference `users.user_id` — either staff or volunteer can record transactions
- `distribution_items` links to `inventories.inventory_id`, not `food_sku`
- `donation_items` unique key includes `expiry_date` — same SKU can appear twice with different expiry
- `user_admin_permissions` references `users.user_id` — any user can hold permissions (schema-only in MVP)
- Volunteer shift overlap is prevented by DB triggers (`SQLSTATE '45000'`)
- The same `food_sku` may exist across multiple branches simultaneously in `inventories`

## Coding Conventions

- Standard Spring Boot packages: `auth`, `config`, `controller`, `dto`, `entity`, `repository`, `service`
- Entity classes map 1:1 to DB tables
- `food_items.sku` maps to `String` in Java, not `Long`
- `food_categories` is a controlled reference table — do not allow free CRUD. Category names must match exactly for `vw_expiring_inventory` perishability logic (Produce, Dairy, Protein, Frozen Foods, Baby Food = perishable)
- Role enum: `STAFF(0), VOLUNTEER(1)` — match `users.role` values
- Insight queries go in the service layer via `@Query` or native SQL — never inline in controllers
- Trigger violations surface as `DataIntegrityViolationException` — parse `MESSAGE_TEXT` for clean frontend errors
- Frontend API calls go through a single `api.js` utility that attaches JWT
- Use `@PreAuthorize("hasRole('STAFF')")` or `SecurityConfig` filter chain to gate reports routes

## Schema Entry Point

DDL: `sql/dbDDL.sql` → sources `dbTRIGGERS.sql` and `dbPROCS.sql`

## API

REST over HTTPS. JWT in `Authorization: Bearer <token>` header.

| Group | Routes | Access |
|---|---|---|
| Auth | `POST /auth/login` | Public |
| Inventory | `/inventory/**` | All |
| Donations | `/donations/**`, `/donation-items/**` | All |
| Distributions | `/distributions/**`, `/distribution-items/**` | All |
| Beneficiaries | `/beneficiaries/**` | All |
| Donors | `/donors/**` | All |
| Volunteers | `/volunteers/**`, `/shifts/**` | All |
| Reports | `/reports/**` | Staff only |

## Out of Scope (MVP)

No self-registration, no password reset, no permission management UI, no beneficiary/donor login, no food inspection tracking, no pagination, no file uploads, no email notifications.

@.claude/rules/schema-tables.md
@.claude/rules/insight-queries.md
