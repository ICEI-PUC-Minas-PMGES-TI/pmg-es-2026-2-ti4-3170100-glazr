package com.glazr.google.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleOidcConfigurationTest {
    @Test
    void enabledGoogleRejectsPlaceholderCredentialsAndNamesTheVariables() {
        assertThatThrownBy(() -> new GoogleOidcConfiguration()
                .googleClientRegistration("invalid-google-client-id", "invalid-google-client-secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GOOGLE_CLIENT_ID")
                .hasMessageContaining("GOOGLE_CLIENT_SECRET");
    }
}
