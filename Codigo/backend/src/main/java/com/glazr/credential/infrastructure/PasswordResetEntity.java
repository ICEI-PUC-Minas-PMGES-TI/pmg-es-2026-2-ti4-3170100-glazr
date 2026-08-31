package com.glazr.credential.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_requests")
public class PasswordResetEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PasswordResetEntity() {
    }

    public static PasswordResetEntity create(UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        if (userId == null || tokenHash == null || !tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Reset inválido");
        }
        if (createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Validade do reset é inválida");
        }
        PasswordResetEntity reset = new PasswordResetEntity();
        reset.id = UUID.randomUUID();
        reset.userId = userId;
        reset.tokenHash = tokenHash;
        reset.expiresAt = expiresAt;
        reset.createdAt = createdAt;
        return reset;
    }

    public boolean isUsableAt(Instant now) {
        return consumedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        if (!isUsableAt(now)) throw new IllegalStateException("Reset não pode ser consumido");
        consumedAt = now;
    }

    public void revoke(Instant now) { if (consumedAt == null && revokedAt == null) revokedAt = now; }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
