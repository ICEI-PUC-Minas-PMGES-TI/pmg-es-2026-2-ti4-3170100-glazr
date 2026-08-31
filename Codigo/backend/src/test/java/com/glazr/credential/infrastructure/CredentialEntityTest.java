package com.glazr.credential.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialEntityTest {

    private static final UUID USER_ID = UUID.fromString("c5ae9190-5328-4c5f-904d-fe7249ce35fd");
    private static final Instant NOW = Instant.parse("2026-08-31T10:00:00Z");

    @Test
    void localPasswordBelongsToExactlyOneUserAndTracksChanges() {
        PasswordCredentialEntity credential = PasswordCredentialEntity.create(USER_ID, "encoded-hash", NOW);

        credential.changePassword("new-encoded-hash", NOW.plusSeconds(60));

        assertThat(credential.getUserId()).isEqualTo(USER_ID);
        assertThat(credential.getPasswordHash()).isEqualTo("new-encoded-hash");
        assertThat(credential.getPasswordChangedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(credential.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsBlankPasswordHashes() {
        assertThatThrownBy(() -> PasswordCredentialEntity.create(USER_ID, " ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void googleIdentityNormalizesEmailAndKeepsSubjectAsPermanentKey() {
        ExternalIdentityEntity identity = ExternalIdentityEntity.linkGoogle(
                USER_ID, "google-subject-123", " USER@Example.Invalid ", NOW);

        identity.recordLogin(NOW.plusSeconds(30));

        assertThat(identity.getProvider()).isEqualTo("GOOGLE");
        assertThat(identity.getSubject()).isEqualTo("google-subject-123");
        assertThat(identity.getEmailAtLink()).isEqualTo("user@example.invalid");
        assertThat(identity.getLastLoginAt()).isEqualTo(NOW.plusSeconds(30));
    }
}
