package com.primaryfeed.entity;

/**
 * Superseded by UserAdminPermission.
 * The SQL schema uses user_admin_permissions (FK to users.user_id),
 * not staff_admin_permissions. This class is intentionally NOT annotated
 * with @Entity so JPA does not try to map it.
 */
public class StaffAdminPermission {
    // Replaced by UserAdminPermission — do not use.
}
