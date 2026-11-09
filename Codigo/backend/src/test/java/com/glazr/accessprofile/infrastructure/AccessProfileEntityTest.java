package com.glazr.accessprofile.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AccessProfileEntityTest {

    @Test
    void preservesTheUsersCapitalizationWhileTrimmingWhitespace() {
        AccessProfileEntity profile = AccessProfileEntity.create(
                "  Vendas internas  ", null, Set.of(), Instant.EPOCH);

        assertThat(profile.getName()).isEqualTo("Vendas internas");
    }
}
