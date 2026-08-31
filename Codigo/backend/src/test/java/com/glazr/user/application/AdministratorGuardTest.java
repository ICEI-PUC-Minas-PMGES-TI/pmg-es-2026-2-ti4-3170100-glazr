package com.glazr.user.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdministratorGuardTest {

    @Test
    void preventsDeactivatingTheLastActiveAdministrator() {
        UserRepository users = mock(UserRepository.class);
        AccessProfileRepository profiles = mock(AccessProfileRepository.class);
        AccessProfileEntity administrator = mock(AccessProfileEntity.class);
        when(administrator.getId()).thenReturn(AccessProfileEntity.ADMINISTRATOR_ID);
        UserEntity user = UserEntity.createEmployee(
                "admin@glazr.local", "Admin", Set.of(administrator), Instant.EPOCH);
        when(users.countActiveByProfileId(AccessProfileEntity.ADMINISTRATOR_ID)).thenReturn(1L);
        when(profiles.findByIdForUpdate(AccessProfileEntity.ADMINISTRATOR_ID))
                .thenReturn(Optional.of(administrator));

        assertThatThrownBy(() -> new AdministratorGuard(users, profiles).assertMayDeactivate(user))
                .isInstanceOf(LastActiveAdministratorException.class);
    }
}
