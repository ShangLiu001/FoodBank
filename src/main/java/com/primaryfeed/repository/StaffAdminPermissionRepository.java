package com.primaryfeed.repository;

/**
 * Superseded by UserAdminPermissionRepository.
 * StaffAdminPermission no longer has @Entity — this interface is intentionally
 * NOT annotated with @Repository so Spring does not register it.
 */
public interface StaffAdminPermissionRepository {
    // Replaced by UserAdminPermissionRepository — do not use.
}
