package com.glazr.invitation.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.credential.application.TemporaryTokenService;
import com.glazr.credential.infrastructure.ExternalIdentityRepository;
import com.glazr.credential.infrastructure.PasswordCredentialEntity;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.invitation.domain.InvitationStatus;
import com.glazr.invitation.infrastructure.EmployeeInvitationEntity;
import com.glazr.invitation.infrastructure.EmployeeInvitationRepository;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationServiceTest {
    @Test
    void acceptsInvitationWithPasswordForTheExistingUser() {
        Instant now = Instant.parse("2026-08-31T12:00:00Z");
        TemporaryTokenService tokens = new TemporaryTokenService();
        var token = tokens.generate();
        AccessProfileEntity profile = AccessProfileEntity.create("Operador", null, Set.of(), now);
        UserEntity user = UserEntity.createEmployee("person@example.com", "Person", Set.of(profile), now);
        EmployeeInvitationEntity invitation = EmployeeInvitationEntity.create(
                user.getId(), UUID.randomUUID(), token.hash(), now.plus(Duration.ofHours(48)), now);
        EmployeeInvitationRepository invitations = mock(EmployeeInvitationRepository.class);
        UserRepository users = mock(UserRepository.class);
        PasswordCredentialRepository passwords = mock(PasswordCredentialRepository.class);
        ExternalIdentityRepository external = mock(ExternalIdentityRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(invitations.findByTokenHashForUpdate(token.hash())).thenReturn(Optional.of(invitation));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwords.existsById(user.getId())).thenReturn(false);
        when(encoder.encode("uma frase senha segura")).thenReturn("hash-seguro");
        InvitationService service = new InvitationService(invitations, users, passwords, external, tokens,
                encoder, mock(ApplicationEventPublisher.class), Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofHours(48), false);

        var authenticated = service.acceptWithPassword(token.rawValue(), "uma frase senha segura");

        assertThat(authenticated.id()).isEqualTo(user.getId());
        assertThat(authenticated.authenticationMethods()).containsExactly("PASSWORD");
        assertThat(invitation.statusAt(now)).isEqualTo(InvitationStatus.CONSUMED);
        verify(passwords).save(any(PasswordCredentialEntity.class));
    }
}
