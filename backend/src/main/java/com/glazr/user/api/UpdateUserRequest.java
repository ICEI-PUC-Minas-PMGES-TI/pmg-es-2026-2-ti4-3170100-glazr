package com.glazr.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UpdateUserRequest(
        @Email @Size(max = 320) String email,
        @Size(min = 1, max = 120) String displayName,
        Set<UUID> accessProfileIds
) {
}
