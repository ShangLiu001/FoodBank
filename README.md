# PrimaryFeed — Food Bank Management System

**CS 5200 · Group 6 · The Primary Keys**

PrimaryFeed is a food bank management system built around a centralized MySQL relational database. The API and frontend are thin shells that expose the database's power — triggers, stored procedures, views, and referential integrity do the heavy lifting.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Running the App](#running-the-app)
3. [Authentication](#authentication)
4. [Roles: User, Staff, Volunteer](#roles-user-staff-volunteer)
5. [People Who Don't Log In: Donors & Beneficiaries](#people-who-dont-log-in-donors--beneficiaries)
6. [Entity Relationship Map](#entity-relationship-map)
7. [Core Logic: Donation → Inventory → Distribution](#core-logic-donation--inventory--distribution)
8. [Inventory Explained](#inventory-explained)
9. [Database Triggers](#database-triggers)
10. [API Reference](#api-reference)
11. [Request Body Reference](#request-body-reference)
12. [Reports (Insight Queries)](#reports-insight-queries)
13. [Error Responses](#error-responses)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.x |
| ORM | Spring Data JPA / Hibernate 6 |
| Database | MySQL 8 |
| Auth | JWT (stateless, Spring Security) |
| Frontend | React (SPA) |
| Deployment | GCP Compute Engine (`http://34.10.48.147:8080`) |

---

## Running the App

```bash
# Build
./mvnw clean package -DskipTests

# Run (local)
./mvnw spring-boot:run
```

The server starts on port `8080`. All API routes are under `/api/`.

---

## Authentication

All endpoints except `POST /api/auth/login` require a JWT in the `Authorization` header.

```
Authorization: Bearer <token>
```

### Login

```
POST /api/auth/login
```

**Request body:**
```json
{
  "email": "alice.nguyen@bafb.org",
  "password": "yourpassword"
}
```

**Response:**
```json
{
  "token": "eyJhbGci...",
  "role": "ROLE_STAFF",
  "userId": 1,
  "name": "Alice Nguyen"
}
```

Save the `token` value. Pass it as `Bearer <token>` in every subsequent request.

**Role values in the JWT:**
- `ROLE_STAFF` — full access including reports
- `ROLE_VOLUNTEER` — access to operational endpoints; no reports

### Get Current User

```
GET /api/auth/me
Authorization: Bearer <token>
```

Returns the full `User` object for the authenticated user.

---

## Roles: User, Staff, Volunteer

### The supertype/subtype pattern

The database uses a supertype/subtype pattern. Every person with a system account is a **User**. Depending on their role, they also have a row in either the **staff** or **volunteers** table that holds role-specific extra fields.

```
users (supertype — holds identity, login credentials, branch assignment)
  ├── staff    (subtype — adds: job_title, hire_date)
  └── volunteers (subtype — adds: availability, background_check)
```

**Creating a user always requires two steps:**
1. `POST /api/users` — creates both the `users` row and the `staff` or `volunteers` subtype row in one API call (see [Request Body Reference](#request-body-reference))
2. The `role` field on the `users` table (`0 = Staff`, `1 = Volunteer`) determines which subtype row is created

**Role cannot be changed after creation.** A DB trigger (`trg_users_prevent_role_change`) raises an error if you try. To change a user's role, you must delete the subtype row first, then update the role, then create the new subtype row.

### Staff (`role = 0`)

Staff can do everything:
- Full CRUD on all entities
- Access to all 17 insight reports (`/api/reports/**`)
- User management (`/api/users/**`)

### Volunteer (`role = 1`)

Volunteers have access to all operational endpoints (inventory, donations, distributions, beneficiaries, donors, shifts) but **cannot access reports**. A `403 Forbidden` is returned if a volunteer hits `/api/reports/**`.

### User fields

| Field | Type | Notes |
|---|---|---|
| `userId` | Integer | Auto-generated PK |
| `firstName` / `lastName` | String | |
| `email` | String | Unique |
| `phone` | String | |
| `role` | Byte | `0` = Staff, `1` = Volunteer |
| `status` | Byte | `0` = Inactive, `1` = Active. Inactive users get `403` on login |
| `branch` | FoodBankBranch | The branch this user is assigned to |
| `address` | Address | Optional home address |
| `drivingLicenseNum` | String | Optional |

---

## People Who Don't Log In: Donors & Beneficiaries

**Donors** and **Beneficiaries** are real-world people or organizations that the food bank interacts with, but they do not have system accounts. Staff and volunteers manage their records on their behalf.

### Donors

A donor is someone who gives food to the food bank.

| Field | Notes |
|---|---|
| `donorId` | Auto PK |
| `donorName` | Full name or org name |
| `donorType` | `0` = Individual, `1` = Organization |
| `email` | Optional, unique |
| `phone` | |
| `address` | Optional |

Donors are connected to the system through **donations** — every donation header references one donor.

### Beneficiaries

A beneficiary is someone who receives food from the food bank.

| Field | Notes |
|---|---|
| `beneficiaryId` | Auto PK |
| `beneficiaryFullName` | |
| `householdSize` | Integer — used for distribution planning |
| `phone` | |
| `email` | Optional, unique |
| `eligibilityStatus` | `0` = Ineligible, `1` = Eligible |
| `address` | Optional |

Beneficiaries are connected to the system through **distributions** — every distribution header references one beneficiary.

---

## Entity Relationship Map

### Complete system map — everything and how it connects

```
┌─────────────┐        ┌──────────────────────────────────────────────────────┐
│  addresses  │        │                        users                         │
│─────────────│        │──────────────────────────────────────────────────────│
│ address_id  │◄───────│ user_id  (PK)                                        │
│ address_    │        │ first_name, last_name                                │
│   line_1/2  │        │ email  (unique)                                      │
│ city, state │        │ phone                                                │
│ zip_code    │        │ password_hash  (write-only)                          │
└─────────────┘        │ role     0=Staff  1=Volunteer                        │
       ▲               │ status   0=Inactive  1=Active                        │
       │               │ branch_id  (FK → food_bank_branches)                 │
       │               │ address_id (FK → addresses, optional)                │
       │               └──────────────────────┬───────────────────────────────┘
       │                                      │ 1
       │                          ┌───────────┴───────────┐
       │                          │ 1                     │ 1
       │                    ┌─────▼──────┐         ┌──────▼──────┐
       │                    │   staff    │         │  volunteers │
       │                    │────────────│         │─────────────│
       │                    │ staff_id   │         │ volunteer_id│
       │                    │ user_id(FK)│         │ user_id (FK)│
       │                    │ job_title  │         │ availability│
       │                    │ hire_date  │         │ background_ │
       │                    └────────────┘         │   check     │
       │                                           └──────┬──────┘
       │                                                  │ 1
       │                                                  │
       │                                         ┌────────▼────────┐
       │                                         │ volunteer_shifts│
       │                                         │─────────────────│
       │                                         │ shift_id        │
       │                                         │ volunteer_id(FK)│
       │                                         │ branch_id   (FK)│
       │                                         │ shift_date      │
       │                                         │ time_start/end  │
       │                                         └─────────────────┘
       │
       │    ┌──────────────┐        ┌───────────────────────────────┐
       └────│   donors     │        │       food_bank_branches      │
            │──────────────│        │───────────────────────────────│
            │ donor_id     │        │ branch_id  (PK)               │
            │ donor_name   │        │ branch_num                    │
            │ donor_type   │        │ branch_name                   │
            │   0=Individual│       │ food_bank_id (FK → food_banks)│
            │   1=Org      │        │ address_id  (FK, optional)    │
            │ email, phone │        └───────────────────────────────┘
            │ address_id   │
            └──────┬───────┘
                   │ 1                         ┌──────────────────┐
                   │                           │  food_categories │
            ┌──────▼──────────────────┐        │──────────────────│
            │        donations        │        │ category_id (PK) │
            │─────────────────────────│        │ category_name    │
            │ donation_id    (PK)     │        │   (READ-ONLY —   │
            │ branch_id      (FK)     │        │    drives expiry  │
            │ donor_id       (FK)     │        │    tier logic)   │
            │ user_id        (FK)     │        └────────┬─────────┘
            │ donation_date           │                 │ 1
            └──────────┬──────────────┘                 │
                       │ 1                       ┌───────▼────────┐
                       │                         │   food_items   │
                ┌──────▼──────────────┐          │────────────────│
                │    donation_items   │          │ sku  (PK, text)│
                │─────────────────────│          │ food_name      │
                │ donation_item_id    │          │ food_desc      │
                │ donation_id    (FK) │          │ storage_cond.  │
                │ food_sku       (FK)─┼──────────► category_id(FK)│
                │ quantity            │          └───────┬────────┘
                │ unit                │                  │ 1
                │ expiry_date         │                  │
                │ UNIQUE(donation_id, │         ┌────────▼────────────────┐
                │   food_sku,         │         │       inventories       │
                │   expiry_date)      │         │─────────────────────────│
                └──────────┬──────────┘         │ inventory_id  (PK)      │
                           │                    │ food_sku      (FK)      │
                    TRIGGER │                    │ branch_id     (FK)      │
              ┌────────────▼─────────────────►  │ quantity   ◄── managed  │
              │  trg_after_donation_items_insert │   by triggers only      │
              │  • find batch (sku+branch+       │ unit                    │
              │    unit+expiry)                  │ expiry_date             │
              │  • EXISTS → UPDATE qty += N      └────────────┬────────────┘
              │  • NOT EXISTS → INSERT new row                │ 1
              └──────────────────────────────────             │
                                                    ┌─────────▼──────────────┐
                                                    │   distribution_items   │
                                                    │────────────────────────│
                                                    │ distribution_item_id   │
                                                    │ distribution_id   (FK) │
                                                    │ inventory_id      (FK) │◄──┐
                                                    │ quantity               │   │
                                                    │ UNIQUE(distribution_id,│   │ TRIGGER
                                                    │        inventory_id)   │   │ trg_after_
                                                    └───────────┬────────────┘   │ distribution_
                                                                │                │ items_insert
                                                         TRIGGER│                │ • qty >= N?
                                                    ┌───────────▼────────┐       │ • UPDATE
                                                    │    distributions   │       │   qty -= N
                                                    │────────────────────│       │ • else 400
                                                    │ distribution_id(PK)│       │
                                                    │ branch_id     (FK) │       │
                                                    │ beneficiary_id(FK) │       │
                                                    │ user_id       (FK) │       │
                                                    │ distribution_date  │       │
                                                    └───────────┬────────┘       │
                                                                │ 1              │
                                                    ┌───────────▼────────┐       │
                                                    │   beneficiaries    │       │
                                                    │────────────────────│       │
                                                    │ beneficiary_id (PK)│       │
                                                    │ beneficiary_       │       │
                                                    │   full_name        │       │
                                                    │ household_size     │       │
                                                    │ phone, email       │       │
                                                    │ eligibility_status │       │
                                                    │   0=Ineligible     │       │
                                                    │   1=Eligible       │       │
                                                    │ address_id    (FK) │       │
                                                    └────────────────────┘       │
                                                                                 │
                                                    (inventory_id FK ────────────┘
                                                     links distribution_items
                                                     directly to a specific batch,
                                                     not just a SKU)
```

---

### Who can log in vs. who is just a record

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SYSTEM PARTICIPANTS                                │
├──────────────────────────────┬──────────────────────────────────────────────┤
│   CAN LOG IN (have accounts) │   CANNOT LOG IN (records only)               │
├──────────────────────────────┼──────────────────────────────────────────────┤
│                              │                                              │
│  users                       │  donors                                      │
│   └── staff    (role = 0)    │   └── give food  → tracked via donations     │
│   └── volunteers(role = 1)   │                                              │
│                              │  beneficiaries                               │
│  Staff: full access          │   └── receive food → tracked via distributions│
│  Volunteer: no reports       │                                              │
│                              │  Staff manage donor/beneficiary records      │
│                              │  on their behalf (no self-service portal)    │
└──────────────────────────────┴──────────────────────────────────────────────┘
```

---

### User → Staff / Volunteer subtype detail

```
                          POST /api/users
                               │
                               ▼
              ┌────────────────────────────────┐
              │  1. INSERT INTO users           │
              │     (identity + credentials)   │
              │     role = 0 or 1              │
              └───────────────┬────────────────┘
                              │
              ┌───────────────▼────────────────┐
              │  2. Check role value            │
              └──────┬─────────────────┬────────┘
                     │ role = 0        │ role = 1
                     ▼                 ▼
        ┌────────────────┐   ┌──────────────────────┐
        │INSERT INTO     │   │INSERT INTO volunteers │
        │staff           │   │                      │
        │────────────────│   │──────────────────────│
        │ user_id (FK)   │   │ user_id  (FK)        │
        │ job_title      │   │ availability         │
        │ hire_date      │   │ background_check     │
        └────────────────┘   └──────────────────────┘
              │                        │
              └───────────┬────────────┘
                          ▼
              DB trigger validates role:
              trg_staff_check_role      → blocks if role != 0
              trg_volunteers_check_role → blocks if role != 1
              trg_users_prevent_role_change → blocks role edits
```

**One API call creates two rows.** The `POST /api/users` endpoint handles both inserts. The DB triggers enforce subtype integrity — you cannot insert a `staff` row for a volunteer-role user, and vice versa.

---

### Food catalog chain

```
food_categories  (READ-ONLY — names drive expiry tier logic)
      │
      │  category_id FK
      ▼
food_items  ──  PK is sku VARCHAR(45), NOT an integer
      │         e.g. "SKU-001", "SKU-011"
      │
      │  food_sku FK
      ▼
inventories  ──  one row per (SKU + branch + expiry_date) batch
      │          quantity is managed exclusively by triggers
      │          the same SKU can appear at multiple branches
      │          the same SKU can appear multiple times at ONE branch
      │          with different expiry_dates (different deliveries)
      │
      │  inventory_id FK
      ▼
distribution_items  ──  references the specific batch, not just the SKU
                        enables FIFO: distribute oldest-expiry batch first
```

**Why `distribution_items` links to `inventory_id` not `food_sku`:**
When you distribute food, you're taking from a specific physical batch — the one expiring soonest. Linking directly to `inventory_id` makes the decrement precise and auditable.

**Why `donation_items` links to `food_sku` not `inventory_id`:**
When food arrives, no inventory batch may exist yet. The trigger creates (or merges into) the right batch automatically based on `(sku, branch, unit, expiry_date)`.

---

### Donation flow — step by step

```
 Staff/Volunteer                  API                      Database
 ───────────────                ───────                  ──────────

 1. Donor arrives         POST /api/donations         INSERT donations
    with food             {branch, donor, user,    →  (gets donation_id)
                           donationDate}

 2. Log each food item    POST /api/donation-items    INSERT donation_items
    in the delivery       {donationId, sku,         →      │
                           quantity, unit,                  │
                           expiryDate}                      │ TRIGGER fires
                                                            ▼
                                              trg_after_donation_items_insert
                                              ┌────────────────────────────┐
                                              │ JOIN donations → branch_id │
                                              │                            │
                                              │ Batch exists?              │
                                              │ (sku + branch + unit +     │
                                              │  expiry match)             │
                                              │                            │
                                              │  YES → UPDATE inventories  │
                                              │        qty = qty + N       │
                                              │                            │
                                              │  NO  → INSERT inventories  │
                                              │        new row, qty = N    │
                                              └────────────────────────────┘

 3. Repeat step 2         POST /api/donation-items    Each item triggers
    for each SKU          (same donationId,           independently.
    in the delivery        different sku/expiry)      Same SKU+expiry
                                                      merges into one batch.
                                                      Different expiry →
                                                      separate batch row.
```

---

### Distribution flow — step by step

```
 Staff/Volunteer                  API                      Database
 ───────────────                ───────                  ──────────

 1. Beneficiary comes     POST /api/distributions     INSERT distributions
    to pick up food       {branch, beneficiary,    →  (gets distribution_id)
                           user, distributionDate}

 2. First check what      GET /api/inventory          Returns all inventory
    is available          /branch/{branchId}          rows at the branch
    at the branch         or                          with quantity > 0.
                          GET /api/reports/1          Pick the inventory_id
                          ?branchId=X                 of the batch to use.

 3. Distribute each       POST /api/distribution-     INSERT distribution_items
    food item             items                    →       │
                          {distributionId,                 │ TRIGGER fires
                           inventoryId,                    ▼
                           quantity}          trg_after_distribution_items_insert
                                              ┌────────────────────────────────┐
                                              │ qty_available =                │
                                              │   SELECT quantity FROM         │
                                              │   inventories                  │
                                              │   WHERE inventory_id = X       │
                                              │                                │
                                              │ qty_available >= requested?    │
                                              │                                │
                                              │  NO  → SIGNAL '45000'          │
                                              │        "Insufficient stock"    │
                                              │        → API returns 400       │
                                              │                                │
                                              │  YES → UPDATE inventories      │
                                              │        qty = qty - requested   │
                                              └────────────────────────────────┘

 4. Repeat step 3         POST /api/distribution-    Each item triggers
    for each food         items (same               independently.
    item given out        distributionId,           UNIQUE(dist_id, inv_id):
                          different inventoryId)    cannot reference the same
                                                    batch twice in one event.
```

---

### The critical asymmetry between donation items and distribution items

```
┌──────────────────────────────┬──────────────────────────────────────────────┐
│      donation_items          │           distribution_items                 │
├──────────────────────────────┼──────────────────────────────────────────────┤
│ References food_sku          │ References inventory_id                      │
│ (the catalog item)           │ (a specific physical batch)                  │
├──────────────────────────────┼──────────────────────────────────────────────┤
│ Trigger FINDS or CREATES     │ Trigger DECREMENTS a row you specified       │
│ the right inventory batch    │ — no lookup needed                           │
├──────────────────────────────┼──────────────────────────────────────────────┤
│ Branch comes from JOIN on    │ Branch is implicit in inventory_id           │
│ the parent donation header   │ (already knows where the stock is)           │
├──────────────────────────────┼──────────────────────────────────────────────┤
│ Unique: (donation_id,        │ Unique: (distribution_id, inventory_id)      │
│          food_sku,           │ One batch per distribution event             │
│          expiry_date)        │                                              │
│ Same SKU allowed twice if    │ Same inventory batch cannot appear twice     │
│ expiry dates differ          │ in the same distribution                     │
├──────────────────────────────┼──────────────────────────────────────────────┤
│ WHY: Food arrives without    │ WHY: Distributing is precise — you pick      │
│ a pre-existing batch.        │ which batch to pull from (FIFO). The         │
│ Trigger handles merge vs.    │ record is an exact audit trail of which      │
│ create automatically.        │ physical stock was given out.                │
└──────────────────────────────┴──────────────────────────────────────────────┘
```

---

### Full dependency map — the whole system at a glance

```
addresses ◄──────────────────────────── users ──────────────┬── staff
                                          │                  └── volunteers ── volunteer_shifts
                                          │                          │
                         ┌────────────────┘                         │
                         │  user_id FK (who logged the event)       │ volunteer_id FK
                         │                                          │
                ┌────────▼─────────┐                       food_bank_branches
                │                  │                                │
          ┌─────▼─────┐     ┌──────▼──────┐ branch_id FK           │
          │ donations │     │distributions│◄───────────────────────┘
          └─────┬─────┘     └──────┬──────┘
                │                  │
         1:N    │           1:N    │
                │                  │
    ┌───────────▼──────┐  ┌────────▼──────────┐
    │  donation_items  │  │ distribution_items │
    │  food_sku (FK)   │  │ inventory_id (FK)  │
    └────────┬─────────┘  └────────────────────┘
             │ TRIGGER                │ TRIGGER
             │ creates/increments     │ decrements
             └──────────►  inventories ◄──────────┘
                              │
                        food_sku (FK)
                              │
                         food_items
                              │
                        category_id (FK)
                              │
                       food_categories
```

---

### One-table CRUD vs. two-table create — summary for FE

```
┌────────────────────────────┬────────────────────────────────────────────────┐
│  Entity                    │  Create pattern                                │
├────────────────────────────┼────────────────────────────────────────────────┤
│  Donor                     │  POST /api/donors  (one call)                  │
│  Beneficiary               │  POST /api/beneficiaries  (one call)           │
│  Food item                 │  POST /api/inventory/food-items  (one call)    │
│  Volunteer shift           │  POST /api/shifts  (one call, trigger checks)  │
├────────────────────────────┼────────────────────────────────────────────────┤
│  User (staff or volunteer) │  POST /api/users  (one call → two DB rows)     │
│                            │  API creates users row + staff/volunteer row   │
├────────────────────────────┼────────────────────────────────────────────────┤
│  Donation                  │  POST /api/donations  ← header first           │
│                            │  POST /api/donation-items  ← one per SKU       │
│                            │  (trigger auto-updates inventory)              │
├────────────────────────────┼────────────────────────────────────────────────┤
│  Distribution              │  POST /api/distributions  ← header first       │
│                            │  POST /api/distribution-items  ← one per batch │
│                            │  (trigger auto-decrements inventory)           │
└────────────────────────────┴────────────────────────────────────────────────┘
```

This is the most important part of the system. The entire food lifecycle flows through three connected concepts.

```
DONOR ──────────► DONATION ──────► INVENTORY ◄────── FOOD ITEMS
                     │                  │                  │
              (staff/volunteer          │           (catalog: SKU,
               logs the intake)         │            name, category)
                                        │
                                        ▼
                               DISTRIBUTION ──────► BENEFICIARY
                                    │
                             (staff/volunteer
                              logs the handout)
```

### Step 1 — Donation (food comes in)

A **donation** is a two-part record:

**Header** (`donations` table): records the event — which branch received it, which donor gave it, which user logged it, and when.

**Line items** (`donation_items` table): the actual food — which SKU, how many, what unit, and what expiry date. One donation can have many line items (e.g., 50 cans of chickpeas AND 30 bags of rice in one delivery).

**When you insert a donation item, a DB trigger fires automatically** (`trg_after_donation_items_insert`). The trigger:
1. Looks for an existing inventory row at the same branch with the same SKU, unit, and expiry date
2. If found → **increments** that row's quantity
3. If not found → **creates** a new inventory row

You never touch `inventories` directly for intake. It is managed entirely by the trigger.

**Important unique constraint:** Within one donation, you cannot add the same `(food_sku, expiry_date)` twice. If you need to add more of the same SKU with the same expiry, update the existing line item. The same SKU with a **different** expiry date is a separate line item (a different batch).

### Step 2 — Inventory (food at rest)

An **inventory** row represents one batch of food at one branch:

| Field | Meaning |
|---|---|
| `inventoryId` | Auto PK — use this when creating distribution items |
| `foodItem.sku` | What food it is |
| `branch.branchId` | Which branch holds it |
| `quantity` | How many units currently in stock |
| `unit` | `cans`, `bags`, `liters`, etc. |
| `expiryDate` | When this specific batch expires |

The same SKU can have multiple inventory rows at the same branch if it was received in separate batches with different expiry dates. This is by design — FIFO (first in, first out) distributes the earliest-expiring batch first.

### Step 3 — Distribution (food goes out)

A **distribution** is also a two-part record:

**Header** (`distributions` table): records the event — which branch gave it out, which beneficiary received it, which user logged it, and when.

**Line items** (`distribution_items` table): which inventory batch was used and how many units. Unlike donation items, distribution items reference `inventoryId` directly — you pick the specific batch, not just the SKU.

**When you insert a distribution item, a DB trigger fires automatically** (`trg_after_distribution_items_insert`). The trigger:
1. Checks that `inventories.quantity >= requested quantity`
2. If not → raises `SQLSTATE '45000'` → Spring returns `400` with the message `"Insufficient stock: quantity would go below 0."`
3. If yes → **decrements** `inventories.quantity` by the requested amount

**Important unique constraint:** Within one distribution, you cannot reference the same `inventoryId` twice. Each distribution header represents one real-world transaction (one beneficiary visit). To distribute from the same batch again — for a different beneficiary or on a different day — create a new distribution header first.

### The two-step pattern for FE developers

Every donation and every distribution is always **two API calls**:

```
# Record a donation
POST /api/donations          ← creates the header, returns donation_id
POST /api/donation-items     ← adds a line item (triggers inventory update)

# Record a distribution
POST /api/distributions      ← creates the header, returns distribution_id
POST /api/distribution-items ← adds a line item (triggers inventory decrement)
```

If a donation has multiple food items, call `POST /api/donation-items` once per line item, all with the same `donationId`.

---

## Inventory Explained

### Viewing inventory

```
GET /api/inventory                    — all inventory rows
GET /api/inventory/{id}               — one row by inventory_id
GET /api/inventory/branch/{branchId}  — all rows at a branch
GET /api/inventory/expiring           — items expiring within 3 days (JVM time)
GET /api/inventory/out-of-stock       — items with quantity = 0
```

### Viewing the food catalog

```
GET /api/inventory/food-items         — all food items (SKU catalog)
GET /api/inventory/food-items/{sku}   — one item by SKU
GET /api/inventory/categories         — all food categories (read-only)
```

**Food categories are read-only.** The category names (`Produce`, `Dairy`, `Protein`, `Frozen Foods`, `Baby Food`) drive a tiered expiry alert system in the `vw_expiring_inventory` database view (7-day alert window for those five, 90-day for everything else). Changing or adding categories will silently break the expiry logic.

### Expiry tiers

| Category | Alert window |
|---|---|
| Produce, Dairy, Protein, Frozen Foods, Baby Food | 7 days |
| Everything else (Canned Goods, Grains, Beverages, etc.) | 90 days |

---

## Database Triggers

These fire automatically — the API does not need to call them explicitly.

| Trigger | Table | When | What it does |
|---|---|---|---|
| `trg_after_donation_items_insert` | `donation_items` | AFTER INSERT | Increments or creates an inventory row for the donated batch |
| `trg_after_distribution_items_insert` | `distribution_items` | AFTER INSERT | Checks stock, then decrements inventory. Raises `45000` if insufficient |
| `trg_staff_check_role` | `staff` | BEFORE INSERT | Prevents inserting a staff row for a user whose `role != 0` |
| `trg_volunteers_check_role` | `volunteers` | BEFORE INSERT | Prevents inserting a volunteer row for a user whose `role != 1` |
| `trg_users_prevent_role_change` | `users` | BEFORE UPDATE | Prevents changing `role` on an existing user |
| `trg_volunteer_shift_no_overlap_insert` | `volunteer_shifts` | BEFORE INSERT | Prevents a volunteer from being booked into overlapping shifts (back-to-back is allowed) |
| `trg_volunteer_shift_no_overlap_update` | `volunteer_shifts` | BEFORE UPDATE | Same overlap check on edits |

When a trigger raises `SQLSTATE '45000'`, Spring catches it as a `DataIntegrityViolationException` and the API returns `400` with the trigger's message text.

---

## API Reference

Base URL: `http://34.10.48.147:8080`

All endpoints except `/api/auth/login` require `Authorization: Bearer <token>`.

### Auth

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Log in, receive JWT |
| `GET` | `/api/auth/me` | All | Get current user's profile |

---

### Users — Staff only

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/users` | All users |
| `GET` | `/api/users/{id}` | User by ID |
| `GET` | `/api/users/role/{role}` | Users by role (`0` = staff, `1` = volunteer) |
| `GET` | `/api/users/branch/{branchId}` | Users assigned to a branch |
| `POST` | `/api/users` | Create user + subtype row (see body below) |
| `PUT` | `/api/users/{id}` | Update user fields (role cannot be changed) |
| `DELETE` | `/api/users/{id}` | Delete user |

---

### Inventory — All authenticated users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/inventory` | All inventory rows |
| `GET` | `/api/inventory/{id}` | Inventory row by ID |
| `GET` | `/api/inventory/branch/{branchId}` | Inventory at a branch |
| `GET` | `/api/inventory/expiring` | Items expiring within 3 days |
| `GET` | `/api/inventory/out-of-stock` | Items with quantity = 0 |
| `POST` | `/api/inventory` | Create inventory row manually |
| `PUT` | `/api/inventory/{id}` | Update inventory row |
| `DELETE` | `/api/inventory/{id}` | Delete inventory row |
| `GET` | `/api/inventory/food-items` | All food items (catalog) |
| `GET` | `/api/inventory/food-items/{sku}` | Food item by SKU |
| `POST` | `/api/inventory/food-items` | Create food item |
| `PUT` | `/api/inventory/food-items/{sku}` | Update food item |
| `DELETE` | `/api/inventory/food-items/{sku}` | Delete food item |
| `GET` | `/api/inventory/categories` | All food categories (read-only) |
| `GET` | `/api/inventory/categories/{id}` | Category by ID |

---

### Donations — All authenticated users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/donations` | All donations |
| `GET` | `/api/donations/{id}` | Donation by ID |
| `GET` | `/api/donations/branch/{branchId}` | Donations at a branch |
| `GET` | `/api/donations/donor/{donorId}` | Donations from a donor |
| `POST` | `/api/donations` | Create donation header |
| `PUT` | `/api/donations/{id}` | Update donation header |
| `DELETE` | `/api/donations/{id}` | Delete donation header |
| `GET` | `/api/donation-items/{id}` | Donation item by ID |
| `GET` | `/api/donation-items/donation/{donationId}` | All items for a donation |
| `POST` | `/api/donation-items` | Add item to donation (triggers inventory) |
| `PUT` | `/api/donation-items/{id}` | Update donation item |
| `DELETE` | `/api/donation-items/{id}` | Delete donation item |

---

### Distributions — All authenticated users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/distributions` | All distributions |
| `GET` | `/api/distributions/{id}` | Distribution by ID |
| `GET` | `/api/distributions/branch/{branchId}` | Distributions at a branch |
| `GET` | `/api/distributions/beneficiary/{beneficiaryId}` | Distributions to a beneficiary |
| `POST` | `/api/distributions` | Create distribution header |
| `PUT` | `/api/distributions/{id}` | Update distribution header |
| `DELETE` | `/api/distributions/{id}` | Delete distribution header |
| `GET` | `/api/distribution-items/{id}` | Distribution item by ID |
| `GET` | `/api/distribution-items/distribution/{distributionId}` | All items for a distribution |
| `POST` | `/api/distribution-items` | Add item to distribution (triggers inventory decrement) |
| `PUT` | `/api/distribution-items/{id}` | Update distribution item |
| `DELETE` | `/api/distribution-items/{id}` | Delete distribution item |

---

### Donors — All authenticated users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/donors` | All donors |
| `GET` | `/api/donors/{id}` | Donor by ID |
| `POST` | `/api/donors` | Create donor |
| `PUT` | `/api/donors/{id}` | Update donor |
| `DELETE` | `/api/donors/{id}` | Delete donor |

---

### Beneficiaries — All authenticated users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/beneficiaries` | All beneficiaries |
| `GET` | `/api/beneficiaries/{id}` | Beneficiary by ID |
| `GET` | `/api/beneficiaries/eligible` | Only eligible beneficiaries |
| `POST` | `/api/beneficiaries` | Create beneficiary |
| `PUT` | `/api/beneficiaries/{id}` | Update beneficiary |
| `DELETE` | `/api/beneficiaries/{id}` | Delete beneficiary |

---

### Volunteers — All authenticated users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/volunteers` | All volunteer profiles |
| `GET` | `/api/volunteers/{id}` | Volunteer profile by volunteer_id |
| `GET` | `/api/volunteers/branch/{branchId}` | Volunteers at a branch |
| `POST` | `/api/volunteers` | Create volunteer profile (user must already exist with role=1) |
| `PUT` | `/api/volunteers/{id}` | Update volunteer profile |
| `DELETE` | `/api/volunteers/{id}` | Delete volunteer profile |

---

### Shifts — All authenticated users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/shifts` | All shifts |
| `GET` | `/api/shifts/{id}` | Shift by ID |
| `GET` | `/api/shifts/volunteer/{volunteerId}` | Shifts for a volunteer |
| `GET` | `/api/shifts/branch/{branchId}` | Shifts at a branch |
| `GET` | `/api/shifts/date/{date}` | Shifts on a date (`YYYY-MM-DD`) |
| `POST` | `/api/shifts` | Schedule a shift (overlap trigger fires) |
| `PUT` | `/api/shifts/{id}` | Update shift (overlap trigger fires) |
| `DELETE` | `/api/shifts/{id}` | Delete shift |

---

### Reports — Staff only

All report endpoints return `List<Map<String, Object>>` — a JSON array of rows with snake_case column names.

| Method | Path | Query Params | Description |
|---|---|---|---|
| `GET` | `/api/reports/1` | `branchId` | Food items currently available at a branch |
| `GET` | `/api/reports/2` | — | Food items expiring within the next 3 days |
| `GET` | `/api/reports/3` | — | Total food quantity across all branches |
| `GET` | `/api/reports/4` | — | Food items grouped by category |
| `GET` | `/api/reports/5` | — | Branch that distributed the most food last month |
| `GET` | `/api/reports/6` | `branchId` | All volunteers assigned to a branch |
| `GET` | `/api/reports/7` | — | Volunteer hours per volunteer (last 30 days) |
| `GET` | `/api/reports/8` | `branchId`, `shiftDate`, `startTime`, `endTime` | Volunteers at a branch during a time window |
| `GET` | `/api/reports/9` | `beneficiaryId` | Distribution history for a beneficiary |
| `GET` | `/api/reports/10` | — | Beneficiaries served per branch this week |
| `GET` | `/api/reports/11` | `donorId` | All donations from a donor |
| `GET` | `/api/reports/12` | — | Food received vs. distributed per branch (net surplus) |
| `GET` | `/api/reports/13` | `threshold` | Food items below a quantity threshold |
| `GET` | `/api/reports/14` | — | All users and their roles |
| `GET` | `/api/reports/15` | — | Food categories most frequently donated or distributed |
| `GET` | `/api/reports/16` | — | Daily distribution totals for the last 14 days |
| `GET` | `/api/reports/17` | — | Branches with highest volunteer-to-distribution ratio |

---

## Request Body Reference

### POST /api/auth/login
```json
{
  "email": "alice.nguyen@bafb.org",
  "password": "yourpassword"
}
```

### POST /api/users — Create Staff User
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@bafb.org",
  "phone": "617-555-0100",
  "password": "plaintext_password",
  "role": 0,
  "status": 1,
  "branchId": 1,
  "jobTitle": "Warehouse Lead",
  "hireDate": "2024-03-01T00:00:00"
}
```

### POST /api/users — Create Volunteer User
```json
{
  "firstName": "Tom",
  "lastName": "Lee",
  "email": "tom.lee@bafb.org",
  "phone": "617-555-0200",
  "password": "plaintext_password",
  "role": 1,
  "status": 1,
  "branchId": 2,
  "availability": "Weekends",
  "backgroundCheck": 1
}
```

### PUT /api/users/{id} — Update User
Only include fields you want to change. `role` is ignored (cannot be changed).
```json
{
  "phone": "617-555-9999",
  "status": 0,
  "password": "new_password_optional"
}
```

### POST /api/donations — Create Donation Header
```json
{
  "branch": { "branchId": 1 },
  "donor":  { "donorId": 2 },
  "user":   { "userId": 1 },
  "donationDate": "2026-04-14T10:00:00"
}
```

### POST /api/donation-items — Add Item to Donation
Triggers `trg_after_donation_items_insert` → inventory updated automatically.
```json
{
  "donation":  { "donationId": 11 },
  "foodItem":  { "sku": "SKU-001" },
  "quantity":  100,
  "unit":      "cans",
  "expiryDate": "2026-12-31T00:00:00"
}
```

### POST /api/distributions — Create Distribution Header
```json
{
  "branch":      { "branchId": 1 },
  "beneficiary": { "beneficiaryId": 3 },
  "user":        { "userId": 1 },
  "distributionDate": "2026-04-14T12:00:00"
}
```

### POST /api/distribution-items — Distribute from Inventory
Triggers `trg_after_distribution_items_insert` → inventory decremented automatically.
Use `inventoryId` (not SKU) — pick the specific batch.
```json
{
  "distribution": { "distributionId": 10 },
  "inventory":    { "inventoryId": 3 },
  "quantity":     20
}
```

### POST /api/donors — Create Donor
```json
{
  "donorName": "Whole Foods Market",
  "donorType": 1,
  "email": "donations@wholefoods.com",
  "phone": "617-555-2000"
}
```

### POST /api/beneficiaries — Create Beneficiary
```json
{
  "beneficiaryFullName": "Maria Gonzalez",
  "householdSize": 3,
  "phone": "617-555-3000",
  "email": "maria.g@email.com",
  "eligibilityStatus": 1
}
```

### POST /api/shifts — Schedule Shift
Overlap trigger fires — returns `400` if the volunteer already has an overlapping shift.
```json
{
  "volunteer":     { "volunteerId": 1 },
  "branch":        { "branchId": 1 },
  "shiftDate":     "2026-04-20",
  "shiftTimeStart": "09:00:00",
  "shiftTimeEnd":  "12:00:00",
  "shiftNotes":    "Sorting incoming donations"
}
```

### POST /api/inventory/food-items — Create Food Item
```json
{
  "sku": "SKU-012",
  "foodName": "Brown Rice (5lb)",
  "foodDescription": "Long grain brown rice",
  "storageCondition": "Cool dry place",
  "category": { "categoryId": 4 }
}
```

---

## Reports (Insight Queries)

Reports always return an array of plain objects with snake_case keys. Example for Q1:

```
GET /api/reports/1?branchId=1
```
```json
[
  {
    "sku": "SKU-001",
    "food_name": "Canned Chickpeas",
    "storage_condition": "Cool dry place",
    "category_name": "Canned Goods",
    "quantity": 118,
    "unit": "cans",
    "expiry_date": "2026-12-31T00:00:00.000+00:00",
    "inventory_id": 2
  }
]
```

Reports that require parameters:

| Report | Required params | Example |
|---|---|---|
| Q1 | `branchId` | `/api/reports/1?branchId=1` |
| Q6 | `branchId` | `/api/reports/6?branchId=2` |
| Q8 | `branchId`, `shiftDate`, `startTime`, `endTime` | `/api/reports/8?branchId=1&shiftDate=2026-04-05&startTime=08:00:00&endTime=13:00:00` |
| Q9 | `beneficiaryId` | `/api/reports/9?beneficiaryId=1` |
| Q11 | `donorId` | `/api/reports/11?donorId=2` |
| Q13 | `threshold` | `/api/reports/13?threshold=50` |

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

## Seed Data (Development)

The DML seed file (`src/main/sql/dbDML.sql`) populates:
- 2 food banks, 10 branches
- 10 users (6 staff, 4 volunteers) across branches
- 11 food items across 10 categories
- 10 donors, 10 beneficiaries
- 10 donations with 14 donation items (triggers create 14 inventory rows)
- 9 distributions with 13 distribution items

**Default seed passwords** are placeholder hashes (`hashed_pw_1`, etc.) — not valid BCrypt. To log in with seed users, run a SQL update with a real BCrypt hash, or create a new user via `POST /api/users` with a plain-text password (the API hashes it for you).

**Key seed IDs for testing:**
- `user_id=1` → Alice Nguyen, Staff, Branch 1 (Downtown Boston)
- `user_id=3` → Carol Lee, Volunteer, Branch 2 (South End)
- `branch_id=1` → Downtown Boston (BAFB)
- `food_sku=SKU-001` → Canned Chickpeas
- `donor_id=2` → Whole Foods Market
- `beneficiary_id=1` → Linda Park (eligible, household size 3)
