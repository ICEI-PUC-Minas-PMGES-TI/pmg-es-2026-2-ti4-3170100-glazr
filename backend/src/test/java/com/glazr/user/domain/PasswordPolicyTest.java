package com.glazr.user.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 11, 129})
    void rejectsPasswordOutsideAcceptedLength(int size) {
        assertThatThrownBy(() -> PasswordPolicy.validate("x".repeat(size)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsPassphraseWithoutArtificialCompositionRules() {
        assertThatCode(() -> PasswordPolicy.validate("uma frase senha longa"))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotTrimPassword() {
        assertThatCode(() -> PasswordPolicy.validate(" 1234567890 "))
                .doesNotThrowAnyException();
    }
}
