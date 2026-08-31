package com.glazr.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PreHashedBCryptPasswordEncoderTest {

    private final PreHashedBCryptPasswordEncoder encoder = new PreHashedBCryptPasswordEncoder(12);

    @Test
    void distinguishesPasswordsThatOnlyDifferAfterBcryptsSeventyTwoByteBoundary() {
        String first = "a".repeat(72);
        String second = first + "b";

        String hash = encoder.encode(first);

        assertThat(encoder.matches(first, hash)).isTrue();
        assertThat(encoder.matches(second, hash)).isFalse();
    }

    @Test
    void supportsOneHundredTwentyEightUnicodeCharacters() {
        String password = "á".repeat(128);
        String hash = encoder.encode(password);

        assertThat(encoder.matches(password, hash)).isTrue();
        assertThat(hash).startsWith("{bcrypt-sha256}");
    }
}
