---
paths:
  - "src/main/java/com/primaryfeed/service/ReportService.java"
  - "src/main/java/com/primaryfeed/controller/ReportController.java"
  - "frontend/src/Reports.jsx"
---

# PrimaryFeed Insight Queries

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
