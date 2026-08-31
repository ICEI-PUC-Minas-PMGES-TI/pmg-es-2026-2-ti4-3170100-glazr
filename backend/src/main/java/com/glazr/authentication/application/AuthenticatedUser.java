package com.glazr.authentication.application;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String displayName,
        boolean active,
        List<String> profiles,
        List<String> permissions
) implements Serializable {
}
