package com.primaryfeed.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps to user_admin_permissions.
 * Grants a permission to any user (staff or volunteer) via user_id → users.user_id.
 * Schema-only in MVP — no UI or service logic is built for this table.
 */
@Data
@Entity
@Table(name = "user_admin_permissions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserAdminPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_permission_id")
    private Integer userPermissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AdminPermission permission;
}
