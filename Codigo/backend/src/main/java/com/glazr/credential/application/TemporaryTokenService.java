package com.glazr.credential.application;

import com.glazr.credential.domain.TemporaryToken;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TemporaryTokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom random;

    public TemporaryTokenService() {
        this(new SecureRandom());
    }

    TemporaryTokenService(SecureRandom random) {
        this.random = random;
    }

    public TemporaryToken generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new TemporaryToken(raw, hash(raw));
    }

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("Token é obrigatório");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
