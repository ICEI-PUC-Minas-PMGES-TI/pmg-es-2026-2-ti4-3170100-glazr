package com.glazr.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.authentication.infrastructure.UserSessionService;
import com.glazr.user.api.CreateUserRequest;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserManagementServiceTest {

    @Test
    void createsAUserWithTheResolvedAccessProfileAndEncodedPassword() {
        Instant now = Instant.parse("2026-08-30T12:00:00Z");
        UserRepository users = mock(UserRepository.class);
        AccessProfileRepository profiles = mock(AccessProfileRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AccessProfileEntity profile = AccessProfileEntity.create("Operador", null, Set.of(), now);
        when(users.findByEmailIgnoreCase("operator@glazr.local")).thenReturn(Optional.empty());
        when(profiles.findAllById(Set.of(profile.getId()))).thenReturn(List.of(profile));
        when(encoder.encode("frase senha inicial")).thenReturn("hash-versionado");
        when(users.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserManagementService service = new UserManagementService(
                users, profiles, encoder, mock(AdministratorGuard.class), mock(UserSessionService.class),
                Clock.fixed(now, ZoneOffset.UTC));

        var created = service.create(new CreateUserRequest(
                " Operator@Glazr.Local ", "Operador", "frase senha inicial", Set.of(profile.getId())));

        assertThat(created.email()).isEqualTo("operator@glazr.local");
        assertThat(created.accessProfiles()).extracting("id").containsExactly(profile.getId());
    }
}
