package com.glazr.credential.application;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryTokenServiceTest {

    private final TemporaryTokenService tokens = new TemporaryTokenService(new SecureRandom());

    @Test
    void generatesAtLeast256BitsAndPersistsOnlyItsSha256Representation() {
        var first = tokens.generate();
        var second = tokens.generate();

        assertThat(first.rawValue()).hasSize(43).doesNotContain("=");
        assertThat(first.hash()).matches("[0-9a-f]{64}");
        assertThat(tokens.hash(first.rawValue())).isEqualTo(first.hash());
        assertThat(second.rawValue()).isNotEqualTo(first.rawValue());
        assertThat(second.hash()).isNotEqualTo(first.hash());
    }

    @Test
    void hashesUsingTheStandardSha256Vector() {
        assertThat(tokens.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
