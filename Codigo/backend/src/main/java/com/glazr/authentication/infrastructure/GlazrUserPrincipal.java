package com.glazr.authentication.infrastructure;

import com.glazr.authentication.application.AuthenticatedUser;

import java.io.Serializable;
import java.security.Principal;

public record GlazrUserPrincipal(AuthenticatedUser user) implements Principal, Serializable {
    @Override
    public String getName() { return user.id().toString(); }
}
