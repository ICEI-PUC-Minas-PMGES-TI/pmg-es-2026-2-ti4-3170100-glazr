package com.glazr.google.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.credential.infrastructure.ExternalIdentityEntity;
import com.glazr.credential.infrastructure.ExternalIdentityRepository;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.invitation.infrastructure.EmployeeInvitationRepository;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleIdentityServiceTest {
    @Test
    void linksGoogleToThePreexistingEmployeeWithoutCreatingAnotherUser() {
        Instant now = Instant.parse("2026-08-31T12:00:00Z");
        UserEntity user = UserEntity.createEmployee("person@example.com", "Person",
                Set.of(AccessProfileEntity.create("Operador", null, Set.of(), now)), now);
        UserRepository users = mock(UserRepository.class);
        ExternalIdentityRepository identities = mock(ExternalIdentityRepository.class);
        PasswordCredentialRepository passwords = mock(PasswordCredentialRepository.class);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(identities.findByProviderAndSubjectForUpdate(ExternalIdentityEntity.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(identities.findByUserIdAndProviderForUpdate(user.getId(), ExternalIdentityEntity.GOOGLE))
                .thenReturn(Optional.empty());
        when(identities.saveAndFlush(any(ExternalIdentityEntity.class))).thenAnswer(call -> call.getArgument(0));
        GoogleIdentityService service = new GoogleIdentityService(users, identities, passwords,
                mock(EmployeeInvitationRepository.class), Clock.fixed(now, ZoneOffset.UTC));

        var authenticated = service.authenticate("google-sub", "PERSON@example.com", true, null, null);

        assertThat(authenticated.id()).isEqualTo(user.getId());
        assertThat(authenticated.authenticationMethods()).containsExactly("GOOGLE");
        verify(identities).saveAndFlush(any(ExternalIdentityEntity.class));
    }

    @Test
    void concurrentGoogleLinkCollisionUsesStableConflictCode() {
        Instant now = Instant.parse("2026-08-31T12:00:00Z");
        UserEntity user = UserEntity.createEmployee("person@example.com", "Person",
                Set.of(AccessProfileEntity.create("Operador", null, Set.of(), now)), now);
        UserRepository users = mock(UserRepository.class);
        ExternalIdentityRepository identities = mock(ExternalIdentityRepository.class);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(identities.findByProviderAndSubjectForUpdate(ExternalIdentityEntity.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(identities.findByUserIdAndProviderForUpdate(user.getId(), ExternalIdentityEntity.GOOGLE))
                .thenReturn(Optional.empty());
        when(identities.saveAndFlush(any(ExternalIdentityEntity.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent identity"));
        GoogleIdentityService service = new GoogleIdentityService(users, identities,
                mock(PasswordCredentialRepository.class), mock(EmployeeInvitationRepository.class),
                Clock.fixed(now, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.authenticate("google-sub", user.getEmail(), true, null, null))
                .isInstanceOfSatisfying(com.glazr.shared.exception.ApplicationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("GOOGLE_IDENTITY_ALREADY_LINKED"));
    }
}
