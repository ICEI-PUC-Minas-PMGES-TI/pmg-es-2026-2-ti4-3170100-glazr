package com.glazr.user.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class EmailAddress {

    private static final int MAX_LENGTH = 320;
    private static final Pattern BASIC_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailAddress() {
    }

    public static String normalize(String email) {
        String normalized = Objects.requireNonNull(email, "email").strip().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("E-mail é obrigatório");
        if (normalized.length() > MAX_LENGTH || !BASIC_EMAIL.matcher(normalized).matches())
            throw new IllegalArgumentException("E-mail inválido");
        return normalized;
    }
}
