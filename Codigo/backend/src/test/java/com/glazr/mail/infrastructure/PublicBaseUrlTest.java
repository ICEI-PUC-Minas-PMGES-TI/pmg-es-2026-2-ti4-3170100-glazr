package com.glazr.mail.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicBaseUrlTest {

    @Test
    void buildsTemporaryLinksOnlyInFragments() {
        PublicBaseUrl baseUrl = new PublicBaseUrl("https://glazr.example.invalid");

        assertThat(baseUrl.invitationLink("raw-token"))
                .isEqualTo("https://glazr.example.invalid/activate#token=raw-token");
        assertThat(baseUrl.passwordResetLink("reset-token"))
                .isEqualTo("https://glazr.example.invalid/reset-password#token=reset-token");
    }

    @Test
    void rejectsUrlsThatCanRedirectOrLeakCredentials() {
        assertThatThrownBy(() -> new PublicBaseUrl("https://user:secret@example.invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicBaseUrl("https://example.invalid?redirect=evil"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicBaseUrl("file:///tmp/glazr"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
