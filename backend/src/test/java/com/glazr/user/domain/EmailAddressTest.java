package com.glazr.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void normalizesExternalWhitespaceAndCase() {
        assertThat(EmailAddress.normalize("  Admin@Glazr.COM \t")).isEqualTo("admin@glazr.com");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> EmailAddress.normalize("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedEmail() {
        assertThatThrownBy(() -> EmailAddress.normalize("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmailLongerThanDatabaseColumn() {
        assertThatThrownBy(() -> EmailAddress.normalize("a".repeat(310) + "@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
