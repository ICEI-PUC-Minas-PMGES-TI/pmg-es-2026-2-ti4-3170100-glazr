package com.glazr.permission.api;

import com.glazr.permission.infrastructure.PermissionEntity;

public record PermissionResponse(String key, String module, String description) {
    public static PermissionResponse from(PermissionEntity permission) {
        return new PermissionResponse(permission.getKey(), permission.getModule(), permission.getDescription());
    }
}
