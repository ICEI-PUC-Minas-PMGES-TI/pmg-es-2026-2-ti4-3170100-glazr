package com.glazr.authentication.application;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String displayName,
        boolean active,
        String accountType,
        List<String> profiles,
        List<String> permissions,
        List<String> authenticationMethods
) implements Serializable {
}
