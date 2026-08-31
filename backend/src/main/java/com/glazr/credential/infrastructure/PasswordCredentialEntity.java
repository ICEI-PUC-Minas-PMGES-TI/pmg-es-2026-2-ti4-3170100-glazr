package com.glazr.credential.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_password_credentials")
public class PasswordCredentialEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PasswordCredentialEntity() {
    }

    public static PasswordCredentialEntity create(UUID userId, String passwordHash, Instant now) {
        if (userId == null) throw new IllegalArgumentException("Usuário é obrigatório");
        validateHash(passwordHash);
        if (now == null) throw new IllegalArgumentException("Data é obrigatória");
        PasswordCredentialEntity credential = new PasswordCredentialEntity();
        credential.userId = userId;
        credential.passwordHash = passwordHash;
        credential.passwordChangedAt = now;
        credential.createdAt = now;
        credential.updatedAt = now;
        return credential;
    }

    public void changePassword(String passwordHash, Instant now) {
        validateHash(passwordHash);
        if (now == null) throw new IllegalArgumentException("Data é obrigatória");
        this.passwordHash = passwordHash;
        this.passwordChangedAt = now;
        this.updatedAt = now;
    }

    private static void validateHash(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Hash de senha é obrigatório");
        if (value.length() > 100) throw new IllegalArgumentException("Hash de senha inválido");
    }

    public UUID getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
