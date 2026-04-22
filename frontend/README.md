# PrimaryFeed — Frontend

React SPA for the PrimaryFeed food bank management system. This guide covers running the frontend standalone with Vite HMR against a local or remote backend — useful for active UI development. For the normal setup where Maven bundles everything into one JAR, see the [main README](../README.md).

## Prerequisites

- Node.js 18+
- A running PrimaryFeed backend — either local (`http://localhost:8080`) or GCP (`http://34.10.48.147:8080`)

## Setup

```bash
cd frontend
npm install
```

Add a dev proxy to `vite.config.js` so Vite forwards `/api/*` requests to your backend. Add the `server.proxy` block (keep the existing `build` block):

```js
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'   // change to GCP URL if using remote backend
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true
  }
})
```

> **Important:** Do not commit this proxy change — it's only needed for the Vite dev server. In bundled mode (Maven build), frontend and backend share the same origin, so relative paths work natively without a proxy.

Then start the dev server:

```bash
npm run dev
```

The app will be available at `http://localhost:5173`. All `/api/*` requests are proxied to the backend — no environment variables needed.

## Login

Use any account from the database. Example accounts:

| Email | Password | Role |
|---|---|---|
| alice.nguyen@bafb.org | test123 | Staff |
| carol.lee@bafb.org | test123 | Volunteer |

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
