# PrimaryFeed — Frontend

React SPA for the PrimaryFeed food bank management system.

## Prerequisites

- Node.js 18+
- The Spring Boot backend running at `http://34.10.48.147:8080`

## Setup

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

## Login

Use any account from the database. Example accounts:

| Email | Password | Role |
|---|---|---|
| carol.lee@bafb.org | password123 | Staff |

Staff accounts see Dashboard and Reports. Volunteer accounts see Operations and Community only.

## Pages

| Route | Access | Description |
|---|---|---|
| `/dashboard` | Staff only | Metric cards, expiring inventory, recent donations |
| `/operations` | All | Inventory, Donations, Distributions, Food Items CRUD |
| `/community` | All | Volunteers, Donors, Beneficiaries, Staff, Shifts CRUD |
| `/reports` | Staff only | 17 pre-built insight queries |

## Tech Stack

- React 19 + Vite 6
- React Router 7
- Tailwind CSS 3
