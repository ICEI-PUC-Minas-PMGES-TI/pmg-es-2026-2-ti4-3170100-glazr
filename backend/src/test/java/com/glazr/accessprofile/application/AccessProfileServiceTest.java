package com.glazr.accessprofile.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.glazr.accessprofile.api.AccessProfileRequest;
import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.permission.infrastructure.PermissionRepository;
import com.glazr.shared.exception.ConflictException;
import com.glazr.user.infrastructure.UserRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccessProfileServiceTest {

    @Test
    void protectsTheSystemAdministratorProfileFromOrdinaryUpdates() {
        AccessProfileRepository profiles = mock(AccessProfileRepository.class);
        AccessProfileEntity administrator = mock(AccessProfileEntity.class);
        when(administrator.isSystemProfile()).thenReturn(true);
        when(profiles.findById(AccessProfileEntity.ADMINISTRATOR_ID)).thenReturn(Optional.of(administrator));
        AccessProfileService service = new AccessProfileService(
                profiles, mock(PermissionRepository.class), mock(UserRepository.class), Clock.systemUTC());

        AccessProfileRequest request = new AccessProfileRequest(
                "Outro nome", "Tentativa", true, Set.of("USER_READ"));

        assertThatThrownBy(() -> service.replace(AccessProfileEntity.ADMINISTRATOR_ID, request))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("PROTECTED_ACCESS_PROFILE");
    }
}
