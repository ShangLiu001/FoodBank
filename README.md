# PrimaryFeed — Food Bank Management System

**CS 5200 · Group 6 · The Primary Keys**

PrimaryFeed is a food bank management system built around a centralized MySQL relational database. The API and frontend are thin shells that expose the database's power — triggers, stored procedures, views, and referential integrity do the heavy lifting.

---

## Table of Contents

1. [Quick Start](#quick-start-local-development) — Setup instructions for local development
2. [Tech Stack](#tech-stack) — Technologies used
3. [Project Structure](#project-structure) — Key directories and SQL files
4. [Authentication & Roles](#authentication--roles) — JWT auth, STAFF vs VOLUNTEER roles
5. [Key Concepts](#key-concepts) — Donation → Inventory → Distribution flow, triggers, design decisions
6. [API Overview](#api-overview) — Endpoint groups (see Swagger for details)
7. [Common Workflows](#common-workflows) — Recording donations, distributions, creating users
8. [Error Responses](#error-responses) — HTTP status codes and trigger errors
9. [Testing](#testing) — Testing and CI/CD
10. [Deployment](#deployment) — Production environment

---

## Quick Start (Local Development)

### Prerequisites

- **Java 25+** (or Java 21+)
- **MySQL 8** (running locally on port 3306 (default))
- **Maven 3.8+**
- **Docker** (required for integration tests — TestContainers spins up a real MySQL instance)

> **Note:** The frontend (React/Vite) is built automatically by Maven via frontend-maven-plugin during mvn package. No separate Node.js install is required. To run the frontend standalone with Vite HMR (e.g. against the GCP backend), see [frontend/README.md](frontend/README.md).

### Database Setup

The fastest way to set up the local database is through `execute.sql`, which sources all SQL files in the correct order:

```bash
mysql -u root -p < src/main/sql/execute.sql
```

This runs the following files in order:

| Step | File | Purpose |
|---|---|---|
| 1 | `dbDDL.sql` | Creates the database (if not exists), tables, views, triggers, and procedures |
| 2 | `dbDML.sql` | Seeds sample data (food banks, branches, users, donors, beneficiaries, inventory) |

> **Order matters:** The DDL file sources `dbTRIGGERS.sql` and `dbPROCS.sql` internally — triggers and procedures must exist before seed data is inserted, since DML inserts fire triggers automatically.

<details>
<summary><strong>Manual setup (if you prefer step-by-step)</strong></summary>

```bash
# 1. Run DDL (creates database, tables, views, triggers, procedures)
mysql -u root -p < src/main/sql/dbDDL.sql

# 2. Run DML (seed data)
mysql -u root -p < src/main/sql/dbDML.sql
```

</details>

<details>
<summary><strong>Resetting the database</strong></summary>

```bash
# Drop all tables and start fresh
mysql -u root -p < src/main/sql/dbDROP.sql

# Then re-run execute.sql
mysql -u root -p < src/main/sql/execute.sql
```

</details>

### Application Setup

1. **Configure database credentials** — create `src/main/resources/application-local.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/primaryfeed
   spring.datasource.username=root
   spring.datasource.password=
   ```

2. **Build:**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Run:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. **Server starts on:** `http://localhost:8080`

### API Documentation

**Swagger UI:** http://localhost:8080/swagger-ui/index.html

All endpoint details, request/response schemas, and interactive testing available in Swagger.

### Test Credentials

| Role | Email | Password |
|---|---|---|
| **STAFF** (full access including reports) | `alice.nguyen@bafb.org` | `test123` |
| **VOLUNTEER** (operational access, no reports) | `bob.tran@bafb.org` | `test123` |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 25 (or Java 21+), Spring Boot 3.5.0 |
| ORM | Spring Data JPA / Hibernate 6 |
| Database | MySQL 8 |
| Auth | JWT (stateless, Spring Security) |
| Frontend | React (SPA), built via `frontend-maven-plugin` |
| Deployment | GCP Compute Engine |

---

## Project Structure

```
FoodBank/
├── frontend/                  # React/Vite SPA (built into Spring Boot JAR by Maven)
├── src/
│   ├── main/
│   │   ├── java/              # Spring Boot controllers, services, entities, security
│   │   ├── resources/         # Application properties, static assets
│   │   └── sql/               # Database scripts (see Database Setup)
│   └── test/                  # Integration tests (TestContainers + real MySQL)
├── .github/workflows/         # CI/CD (test suite + GCP deploy)
└── pom.xml                    # Maven build (includes frontend-maven-plugin)
```

## Authentication & Roles

All endpoints except `POST /api/auth/login` require a JWT in the `Authorization` header:

```
Authorization: Bearer <token>
```

**Login:** `POST /api/auth/login`

**Response includes:**
- `token` — JWT to use in subsequent requests
- `role` — `ROLE_STAFF` or `ROLE_VOLUNTEER`
- `userId`, `name`

### Roles

The system has two roles defined by `users.role`:

| Role | Value | Access |
|---|---|---|
| **STAFF** | `0` | Full access to all endpoints including `/api/reports/**` and `/api/users/**` |
| **VOLUNTEER** | `1` | Access to operational endpoints (inventory, donations, distributions, shifts). **No access to reports** (403) |

**Role cannot be changed after user creation.** A DB trigger (`trg_users_prevent_role_change`) enforces this.

### Supertype/Subtype Pattern

Every user account follows a supertype/subtype pattern:

```
users (supertype — identity, credentials, branch assignment)
  ├── staff      (subtype — adds: job_title, hire_date)
  └── volunteers (subtype — adds: availability, background_check)
```

**Creating a user:** `POST /api/users` creates both the `users` row AND the appropriate subtype row (staff or volunteers) in one API call. The `role` field determines which subtype is created.

### Non-Login Entities

**Donors** and **Beneficiaries** do not have system accounts. They are records managed by staff/volunteers on their behalf:

- **Donors** (`donorType`: `0` = Individual, `1` = Organization) — give food, tracked via donations
- **Beneficiaries** (`eligibilityStatus`: `0` = Ineligible, `1` = Eligible) — receive food, tracked via distributions

---

## Key Concepts

### Donation → Inventory → Distribution Flow

The system's core functionality revolves around three connected tables:

```
DONOR ──► DONATION ──► INVENTORY ◄── DISTRIBUTION ──► BENEFICIARY
              │            ▲              │
              │  (trigger) │   (trigger)  │
              └────────────┴──────────────┘
```

**Key principle:** Food intake and distribution are recorded as two-part transactions (header + line items). Database triggers automatically manage inventory based on these transactions.

#### Recording a Donation (Food Comes In)

```
1. POST /api/donations          → creates header, returns donation_id
2. POST /api/donation-items     → adds line item (one per SKU)
                                  ↓
                         TRIGGER: trg_after_donation_items_insert
                         - Finds or creates inventory batch
                         - Increments quantity if batch exists
                         - Creates new inventory row if not
```

**Important:** `donation_items` references `food_sku` (the catalog item). The trigger determines which inventory batch to update based on (sku + branch + unit + expiry_date).

#### Recording a Distribution (Food Goes Out)

```
1. GET /api/inventory/branch/{id}  → see available stock, pick inventory_id
2. POST /api/distributions         → creates header, returns distribution_id
3. POST /api/distribution-items    → adds line item (one per batch)
                                     ↓
                          TRIGGER: trg_after_distribution_items_insert
                          - Checks quantity >= requested
                          - Decrements inventory if sufficient
                          - Raises error (400) if insufficient
```

**Important:** `distribution_items` references `inventory_id` (specific batch), not `food_sku`. This enables FIFO — distribute the oldest batch first.

### Database Triggers

| Trigger | Fires On | Purpose |
|---|---|---|
| `trg_after_donation_items_insert` | INSERT `donation_items` | Creates or increments inventory batch |
| `trg_after_distribution_items_insert` | INSERT `distribution_items` | Validates stock and decrements inventory |
| `trg_staff_check_role` | INSERT `staff` | Blocks if `users.role != 0` |
| `trg_volunteers_check_role` | INSERT `volunteers` | Blocks if `users.role != 1` |
| `trg_users_prevent_role_change` | UPDATE `users` | Prevents changing `role` after creation |
| `trg_volunteer_shift_no_overlap_*` | INSERT/UPDATE `volunteer_shifts` | Prevents overlapping shifts (back-to-back allowed) |

When a trigger raises `SQLSTATE '45000'`, the API returns 400 with the trigger's error message.

### Stored Procedures

| Procedure | Purpose |
|---|---|
| `record_donation` | Inserts a donation header + line item and creates or increments the corresponding inventory batch in a single transaction |
| `record_distribution` | Inserts a distribution header + line item, validates stock, and decrements inventory using FIFO (oldest expiry first) in a single transaction |

Example calls and verification queries are in `src/main/sql/dbPROCSCALL.sql`.

### Views

| View | Purpose |
|---|---|
| `vw_expiring_inventory` | Items with `quantity > 0` expiring within 3 months, with computed `days_until_expiry`. Expiry tiers: perishable categories (Produce/Dairy/Meat/Seafood/Bakery) = 7 days; others = 90 days |
| `vw_volunteer_hours_log` | Volunteer shifts joined with user names, with `total_hours` formatted as `Xh Ym` |

### Critical Design Decisions

- **`food_items.sku` is VARCHAR(45)**, not an integer (e.g., "SKU-001")
- **Same SKU can exist at multiple branches** with different quantities and expiry dates
- **Same SKU can appear multiple times at ONE branch** if received in different batches (different expiry dates)
- **`food_categories` is READ-ONLY** — category names drive the expiry tier logic in `vw_expiring_inventory`
- **`donation_items` unique constraint:** (donation_id, food_sku, expiry_date) — same SKU allowed if expiry differs
- **`distribution_items` unique constraint:** (distribution_id, inventory_id) — cannot use same batch twice in one distribution

---

## API Overview

**Complete API documentation with interactive testing:** http://localhost:8080/swagger-ui/index.html

All endpoints except `/api/auth/login` require `Authorization: Bearer <token>`.

| Group | Base Path | Access | Notes |
|---|---|---|---|
| **Auth** | `/api/auth/*` | Public/All | Login (public), get current user |
| **Users** | `/api/users/*` | STAFF only | User management, create with subtype |
| **Inventory** | `/api/inventory/*` | All | Inventory rows, food items, categories |
| **Donations** | `/api/donations/*` | All | Donation headers |
| **Donation Items** | `/api/donation-items/*` | All | Donation line items (triggers inventory++) |
| **Distributions** | `/api/distributions/*` | All | Distribution headers |
| **Distribution Items** | `/api/distribution-items/*` | All | Distribution line items (triggers inventory--) |
| **Donors** | `/api/donors/*` | All | Donor management |
| **Beneficiaries** | `/api/beneficiaries/*` | All | Beneficiary management |
| **Volunteers** | `/api/volunteers/*` | All | Volunteer profile management |
| **Shifts** | `/api/shifts/*` | All | Volunteer shift scheduling |
| **Reports** | `/api/reports/*` | STAFF only | 17 insight queries (Q1-Q17) |

---

## Common Workflows

### Recording a Donation

1. **Create donation header:** `POST /api/donations` (returns `donation_id`)
2. **Add line items:** `POST /api/donation-items` (one call per SKU, triggers auto-update inventory)
3. **Repeat step 2** for each SKU in the delivery

**Key fields:**
- `donation`: references header by `donation_id`
- `foodItem`: references catalog by `sku` (e.g., "SKU-001")
- Trigger creates/increments inventory automatically

### Recording a Distribution

1. **Check available stock:** `GET /api/inventory/branch/{branchId}`
2. **Create distribution header:** `POST /api/distributions` (returns `distribution_id`)
3. **Add line items:** `POST /api/distribution-items` (one call per batch, triggers auto-decrement inventory)
4. **Repeat step 3** for each food item

**Key fields:**
- `distribution`: references header by `distribution_id`
- `inventory`: references specific batch by `inventory_id` (NOT sku!)
- Trigger validates stock and decrements automatically

### Creating a User

**One call creates both users row AND staff/volunteers subtype row:**

`POST /api/users` with:
- `role`: `0` = Staff, `1` = Volunteer
- Staff requires: `jobTitle`, optional `hireDate`
- Volunteer requires: `availability`, optional `backgroundCheck`
- Password is automatically bcrypt hashed

**Role cannot be changed after creation** (DB trigger enforces this).

---

## Error Responses

All error responses are JSON:
```json
{ "error": "message here" }
```

| HTTP Status | When |
|---|---|
| `400` | Validation failure, duplicate key, or **DB trigger violation** (insufficient stock, overlapping shift, role mismatch) |
| `401` | Bad credentials on login |
| `403` | Account inactive, or volunteer accessing staff-only endpoint |
| `404` | Resource not found |
| `500` | Database error (column error, connection failure, etc.) |

### Trigger errors surface as 400

When the DB raises `SQLSTATE '45000'`, Spring catches it as `DataIntegrityViolationException`. `GlobalExceptionHandler` extracts the trigger's `MESSAGE_TEXT` and returns it as the `error` field with HTTP `400`. Frontend code can display this message directly to the user.

Examples:
```json
{ "error": "Insufficient stock: quantity would go below 0." }
{ "error": "Volunteer already has an overlapping shift on this date." }
{ "error": "Cannot insert into staff: user role is not Staff (0)." }
{ "error": "Cannot change user role directly. Delete the subtype row first, then update the role." }
```

---

## Testing

**21 integration tests** using TestContainers (real MySQL 8.0 in Docker) to validate triggers, authentication, and workflows.

### Running Tests

```bash
mvn test  # Requires Docker to be running
```

```bash
# Run a specific test class
mvn test -Dtest=DonationFlowIntegrationTest
```

```bash
# Run with verbose output
mvn test -X
```

### Coverage
Test reports are published automatically by GitHub Actions on every push and PR. See the Test Results check on any commit or pull request for detailed per-test output.

**Test data:**
Tests use dedicated test-specific data to avoid conflicts with seed data:
- Test users: `user_id` 1000+ (staff@test.com, volunteer@test.com)
- Test food items: `TEST-001`, `TEST-002`, `TEST-003`, `TEST-004` (SKU prefix to avoid production SKU conflicts)

### CI/CD

GitHub Actions workflow (`.github/workflows/test.yml`) runs tests on push/PR to `main` or `develop`. Test reports published automatically.

**Troubleshooting:** If tests fail with "Could not find Docker", start Docker Desktop first (`open -a Docker` on macOS).

---

## Deployment

**Production:** GCP Compute Engine at `http://34.10.48.147:8080`

Database schema (`dbDDL.sql`), triggers (`dbTRIGGERS.sql`), procedures (`dbPROCS.sql`), and seed data (`dbDML.sql`) are located in `src/main/sql/`.
