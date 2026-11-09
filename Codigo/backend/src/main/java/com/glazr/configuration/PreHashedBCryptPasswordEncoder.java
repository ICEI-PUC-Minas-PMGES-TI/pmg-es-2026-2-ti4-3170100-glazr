package com.glazr.configuration;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PreHashedBCryptPasswordEncoder implements PasswordEncoder {

    public static final String PREFIX = "{bcrypt-sha256}";

    private final BCryptPasswordEncoder delegate;

    public PreHashedBCryptPasswordEncoder(int strength) {
        this.delegate = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return PREFIX + delegate.encode(preHash(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || !encodedPassword.startsWith(PREFIX)) {
            return false;
        }
        return delegate.matches(preHash(rawPassword), encodedPassword.substring(PREFIX.length()));
    }

    private static String preHash(CharSequence rawPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
