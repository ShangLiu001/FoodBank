---
name: Schema-entity traps in PrimaryFeed
description: Known mismatches between the SQL schema and Java entities that were found and fixed during audit
type: project
---

The SQL schema differs from what the original entity classes assumed in two important ways:

**donations and distributions use user_id, not staff_id**
- SQL: `donations.user_id INT REFERENCES users(user_id)` (any user — staff OR volunteer — can log a donation)
- SQL: `distributions.user_id INT REFERENCES users(user_id)` (same)
- The original entities had `@JoinColumn(name = "staff_id") private Staff staff` — this was wrong and caused boot-time failures
- Fixed: both entities now have `@JoinColumn(name = "user_id") private User user`

**Why:** CLAUDE.md §Key DB Assumptions #8 explicitly states "all food intake and distribution actions must be performed by a registered user (staff or volunteer)" — the FK is to `users`, not `staff`.

**How to apply:** When writing any service/controller/query code that touches donations or distributions, the field is `user`, not `staff`. Repository derived queries use `findByUser_UserId`, not `findByStaff_StaffId`.

---

**user_admin_permissions ≠ staff_admin_permissions**
- SQL has `user_admin_permissions` with `user_id → users(user_id)` and PK `user_permission_id`
- Original entity `StaffAdminPermission` mapped to non-existent table `staff_admin_permissions` with `staff_id → staff`
- Fixed: created `UserAdminPermission.java` and `UserAdminPermissionRepository.java`; neutralized old files
- This table is schema-only in MVP (no service/controller built for it)
