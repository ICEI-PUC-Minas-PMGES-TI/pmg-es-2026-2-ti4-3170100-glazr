package com.glazr.permission.application;

import com.glazr.permission.api.PermissionResponse;
import com.glazr.permission.infrastructure.PermissionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionQueryService {
    private final PermissionRepository permissions;

    public PermissionQueryService(PermissionRepository permissions) { this.permissions = permissions; }

    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Transactional(readOnly = true)
    public List<PermissionResponse> list() {
        return permissions.findAllByOrderByModuleAscKeyAsc().stream().map(PermissionResponse::from).toList();
    }
}
