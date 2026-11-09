package com.glazr.user.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.authentication.infrastructure.UserSessionService;
import com.glazr.credential.application.TemporaryTokenService;
import com.glazr.credential.infrastructure.ExternalIdentityRepository;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.invitation.infrastructure.EmployeeInvitationEntity;
import com.glazr.invitation.infrastructure.EmployeeInvitationRepository;
import com.glazr.user.api.CreateUserRequest;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserManagementServiceTest {

    @Test
    void createsAUserWithAnInvitationAndWithoutAnInitialPassword() {
        Instant now = Instant.parse("2026-08-30T12:00:00Z");
        UserRepository users = mock(UserRepository.class);
        AccessProfileRepository profiles = mock(AccessProfileRepository.class);
        PasswordCredentialRepository credentials = mock(PasswordCredentialRepository.class);
        ExternalIdentityRepository externalIdentities = mock(ExternalIdentityRepository.class);
        EmployeeInvitationRepository invitations = mock(EmployeeInvitationRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        AccessProfileEntity profile = AccessProfileEntity.create("Operador", null, Set.of(), now);
        when(users.findByEmailIgnoreCase("operator@glazr.local")).thenReturn(Optional.empty());
        when(profiles.findAllById(Set.of(profile.getId()))).thenReturn(List.of(profile));
        when(users.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invitations.save(any(EmployeeInvitationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserManagementService service = new UserManagementService(
                users, profiles, credentials, externalIdentities, invitations,
                new TemporaryTokenService(), events,
                mock(AdministratorGuard.class), mock(UserSessionService.class),
                Clock.fixed(now, ZoneOffset.UTC), java.time.Duration.ofHours(48));

        var created = service.create(new CreateUserRequest(
                " Operator@Glazr.Local ", "Operador", Set.of(profile.getId())), UUID.randomUUID());

        assertThat(created.email()).isEqualTo("operator@glazr.local");
        assertThat(created.accessProfiles()).extracting("id").containsExactly(profile.getId());
        assertThat(created.invitationStatus()).isEqualTo("PENDING");
        assertThat(created.authenticationMethods()).isEmpty();
    }
}
