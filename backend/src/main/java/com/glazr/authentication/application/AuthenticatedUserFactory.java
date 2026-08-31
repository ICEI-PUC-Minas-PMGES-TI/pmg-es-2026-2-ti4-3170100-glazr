package com.glazr.authentication.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.permission.infrastructure.PermissionEntity;
import com.glazr.user.infrastructure.UserEntity;

import java.util.List;
import java.util.TreeSet;

public final class AuthenticatedUserFactory {

    private AuthenticatedUserFactory() {
    }

    public static AuthenticatedUser from(UserEntity user) {
        TreeSet<String> profiles = new TreeSet<>();
        TreeSet<String> permissions = new TreeSet<>();
        user.getAccessProfiles().stream()
                .filter(AccessProfileEntity::isActive)
                .forEach(profile -> {
                    profiles.add(profile.getName());
                    profile.getPermissions().stream().map(PermissionEntity::getKey).forEach(permissions::add);
                });
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName(), user.isActive(),
                List.copyOf(profiles), List.copyOf(permissions));
    }
}
