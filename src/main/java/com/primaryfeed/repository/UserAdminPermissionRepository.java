package com.primaryfeed.repository;

import com.primaryfeed.entity.UserAdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAdminPermissionRepository extends JpaRepository<UserAdminPermission, Integer> {
    List<UserAdminPermission> findByUser_UserId(Integer userId);
}
