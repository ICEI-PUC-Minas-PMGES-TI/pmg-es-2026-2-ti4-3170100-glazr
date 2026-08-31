package com.glazr.user.api;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.user.infrastructure.UserEntity;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        boolean active,
        String accountType,
        List<String> authenticationMethods,
        List<ProfileSummary> accessProfiles,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(UserEntity user, List<String> authenticationMethods) {
        List<ProfileSummary> profiles = user.getAccessProfiles().stream()
                .sorted(Comparator.comparing(AccessProfileEntity::getName))
                .map(profile -> new ProfileSummary(profile.getId(), profile.getName()))
                .toList();
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isActive(),
                user.getAccountType().name(), List.copyOf(authenticationMethods), profiles,
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    public record ProfileSummary(UUID id, String name) {
    }
}
