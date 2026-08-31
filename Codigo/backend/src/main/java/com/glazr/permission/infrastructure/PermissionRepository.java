package com.glazr.permission.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {
    List<PermissionEntity> findAllByOrderByModuleAscKeyAsc();
}
