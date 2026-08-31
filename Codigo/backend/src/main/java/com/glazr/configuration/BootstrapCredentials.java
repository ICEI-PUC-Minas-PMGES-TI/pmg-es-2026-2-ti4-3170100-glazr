package com.glazr.configuration;

import java.util.ArrayList;
import java.util.List;

public record BootstrapCredentials(String email, String password, String name) {

    public void validateComplete() {
        List<String> missing = new ArrayList<>();
        if (email == null || email.isBlank()) missing.add("GLAZR_BOOTSTRAP_ADMIN_EMAIL");
        if (password == null || password.isEmpty()) missing.add("GLAZR_BOOTSTRAP_ADMIN_PASSWORD");
        if (name == null || name.isBlank()) missing.add("GLAZR_BOOTSTRAP_ADMIN_NAME");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Banco vazio: configure " + String.join(", ", missing));
        }
    }
}
