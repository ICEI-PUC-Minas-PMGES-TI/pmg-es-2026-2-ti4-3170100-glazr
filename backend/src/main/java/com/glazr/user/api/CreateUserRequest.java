package com.glazr.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 120) String displayName,
        @NotNull @Size(min = 12, max = 128) String password,
        @NotEmpty Set<UUID> accessProfileIds
) {
}
