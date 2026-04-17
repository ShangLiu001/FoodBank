# PrimaryFeed — Frontend

React SPA for the PrimaryFeed food bank management system. This guide covers running the frontend standalone against a remote API (e.g. the GCP-hosted backend). For bundled mode where the frontend is built into the Spring Boot JAR, see the [main README](../README.md).

## Prerequisites

- Node.js 18+
- A running PrimaryFeed backend (e.g. GCP at `http://34.10.48.147:8080`)

## Setup

```bash
cd frontend
npm install
```

Configure Vite to proxy API requests to your backend. In `vite.config.js`:

```js
export default defineConfig({
  server: {
    proxy: {
      '/api': 'http://34.10.48.147:8080'  // or http://localhost:8080 for local backend
    }
  }
})
```

Then start the dev server:

```bash
npm run dev
```

The app will be available at `http://localhost:5173`. All `/api/*` requests are proxied to the backend, so the frontend code uses relative paths (e.g. `fetch('/api/auth/login')`) in both standalone and bundled mode — no environment variables needed.

> **Note:** The proxy config only applies to the Vite dev server. In bundled mode (built via Maven into the Spring Boot JAR), frontend and backend share the same origin so relative paths work natively.

## Login

Use any account from the database. Example accounts:

| Email | Password | Role |
|---|---|---|
| alice.nguyen@bafb.org | password123 | Staff |
| bob.tran@bafb.org | password123 | Volunteer |

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
