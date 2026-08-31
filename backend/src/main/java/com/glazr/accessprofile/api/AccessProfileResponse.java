package com.glazr.accessprofile.api;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.permission.infrastructure.PermissionEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccessProfileResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        boolean systemProfile,
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {
    public static AccessProfileResponse from(AccessProfileEntity profile) {
        return new AccessProfileResponse(profile.getId(), profile.getName(), profile.getDescription(),
                profile.isActive(), profile.isSystemProfile(),
                profile.getPermissions().stream().map(PermissionEntity::getKey).sorted().toList(),
                profile.getCreatedAt(), profile.getUpdatedAt());
    }
}
