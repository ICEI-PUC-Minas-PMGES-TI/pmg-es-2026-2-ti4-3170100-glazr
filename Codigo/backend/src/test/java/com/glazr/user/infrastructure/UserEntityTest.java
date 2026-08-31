package com.glazr.user.infrastructure;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.user.domain.AccountType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEntityTest {

    @Test
    void expiredLockStartsANewFailureSequence() {
        Instant start = Instant.parse("2026-08-30T10:00:00Z");
        UserEntity user = UserEntity.createEmployee("user@glazr.local", "User",
                Set.of(AccessProfileEntity.create("OPERADOR", null, Set.of(), start)), start);
        for (int i = 0; i < 5; i++) user.recordFailedLogin(start);

        user.recordFailedLogin(start.plusSeconds(16 * 60));

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void rejectsDisplayNameLongerThanDatabaseColumn() {
        Instant now = Instant.EPOCH;
        AccessProfileEntity profile = AccessProfileEntity.create("Operador", null, Set.of(), now);

        assertThatThrownBy(() -> UserEntity.createEmployee(
                "user@glazr.local", "x".repeat(121), Set.of(profile), now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsEmployeeWithoutEmbeddingAnAuthenticationCredential() {
        Instant now = Instant.EPOCH;
        AccessProfileEntity profile = AccessProfileEntity.create("Operador", null, Set.of(), now);

        UserEntity user = UserEntity.createEmployee(
                " FUNCIONARIO@EXAMPLE.INVALID ", " Funcionário ", Set.of(profile), now);

        assertThat(user.getEmail()).isEqualTo("funcionario@example.invalid");
        assertThat(user.getDisplayName()).isEqualTo("Funcionário");
        assertThat(user.getAccountType()).isEqualTo(AccountType.EMPLOYEE);
        assertThat(user.isActive()).isTrue();
    }
}
