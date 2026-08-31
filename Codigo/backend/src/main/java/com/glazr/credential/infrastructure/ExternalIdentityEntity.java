package com.glazr.credential.infrastructure;

import com.glazr.user.domain.EmailAddress;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_external_identities")
public class ExternalIdentityEntity {

    public static final String GOOGLE = "GOOGLE";

    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false, length = 30)
    private String provider;
    @Column(nullable = false, length = 255)
    private String subject;
    @Column(name = "email_at_link", nullable = false, length = 320)
    private String emailAtLink;
    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected ExternalIdentityEntity() {
    }

    public static ExternalIdentityEntity linkGoogle(UUID userId, String subject, String email, Instant now) {
        if (userId == null) throw new IllegalArgumentException("Usuário é obrigatório");
        if (subject == null || subject.isBlank() || subject.length() > 255) {
            throw new IllegalArgumentException("Identidade Google inválida");
        }
        if (now == null) throw new IllegalArgumentException("Data é obrigatória");
        ExternalIdentityEntity identity = new ExternalIdentityEntity();
        identity.id = UUID.randomUUID();
        identity.userId = userId;
        identity.provider = GOOGLE;
        identity.subject = subject;
        identity.emailAtLink = EmailAddress.normalize(email);
        identity.linkedAt = now;
        return identity;
    }

    public void recordLogin(Instant now) {
        if (now == null) throw new IllegalArgumentException("Data é obrigatória");
        lastLoginAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getSubject() { return subject; }
    public String getEmailAtLink() { return emailAtLink; }
    public Instant getLinkedAt() { return linkedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
