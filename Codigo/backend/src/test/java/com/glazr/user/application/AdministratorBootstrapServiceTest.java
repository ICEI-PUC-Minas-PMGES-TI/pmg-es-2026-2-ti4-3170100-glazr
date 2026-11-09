package com.glazr.user.application;

import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.configuration.BootstrapCredentials;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.permission.infrastructure.PermissionRepository;
import com.glazr.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdministratorBootstrapServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AccessProfileRepository profiles = mock(AccessProfileRepository.class);
    private final AdministratorBootstrapService service = new AdministratorBootstrapService(
            users, profiles, mock(PermissionRepository.class),
            mock(PasswordEncoder.class), mock(PasswordCredentialRepository.class), Clock.systemUTC());

    @Test
    void failsSafelyWithNamesOfMissingVariablesWhenDatabaseIsEmpty() {
        when(profiles.findByIdForUpdate(com.glazr.accessprofile.infrastructure.AccessProfileEntity.ADMINISTRATOR_ID))
                .thenReturn(Optional.of(mock(com.glazr.accessprofile.infrastructure.AccessProfileEntity.class)));
        when(users.count()).thenReturn(0L);

        assertThatThrownBy(() -> service.bootstrap(
                () -> new BootstrapCredentials("admin@example.invalid", null, "Admin")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GLAZR_BOOTSTRAP_ADMIN_PASSWORD")
                .hasMessageNotContaining("admin@example.invalid");
    }

    @Test
    void ignoresBootstrapValuesWhenUsersAlreadyExist() {
        when(profiles.findByIdForUpdate(com.glazr.accessprofile.infrastructure.AccessProfileEntity.ADMINISTRATOR_ID))
                .thenReturn(Optional.of(mock(com.glazr.accessprofile.infrastructure.AccessProfileEntity.class)));
        when(users.count()).thenReturn(1L);
        AtomicBoolean materialized = new AtomicBoolean();

        assertThat(service.bootstrap(() -> {
            materialized.set(true);
            return new BootstrapCredentials(null, null, null);
        }))
                .isEqualTo(AdministratorBootstrapService.Outcome.SKIPPED_EXISTING_USERS);
        assertThat(materialized).isFalse();
    }
}
