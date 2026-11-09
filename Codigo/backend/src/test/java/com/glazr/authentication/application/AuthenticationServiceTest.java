package com.glazr.authentication.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.credential.infrastructure.ExternalIdentityRepository;
import com.glazr.credential.infrastructure.PasswordCredentialEntity;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordCredentialRepository credentials = mock(PasswordCredentialRepository.class);
    private final ExternalIdentityRepository externalIdentities = mock(ExternalIdentityRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private AuthenticationService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        when(encoder.encode(anyString())).thenReturn("{bcrypt-sha256}$2a$12$sentinel");
        AccessProfileEntity profile = AccessProfileEntity.create("OPERADOR", null, Set.of(), NOW);
        user = UserEntity.createEmployee("admin@glazr.local", "Admin", Set.of(profile), NOW);
        PasswordCredentialEntity credential = PasswordCredentialEntity.create(user.getId(), "hash", NOW);
        service = new AuthenticationService(users, credentials, encoder,
                new AuthenticationTransaction(users, credentials, externalIdentities, clock));
        when(users.findByEmailIgnoreCase("admin@glazr.local")).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(credentials.findById(user.getId())).thenReturn(Optional.of(credential));
        when(credentials.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(credential));
    }

    @Test
    void locksForFifteenMinutesAfterFifthInvalidPassword() {
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.authenticate("admin@glazr.local", "senha incorreta"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isEqualTo(NOW.plusSeconds(15 * 60));
    }

    @Test
    void successfulAuthenticationClearsPreviousFailures() {
        user.recordFailedLogin(NOW.minusSeconds(1));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);

        AuthenticatedUser authenticated = service.authenticate(" ADMIN@GLAZR.LOCAL ", "senha válida longa");

        assertThat(authenticated.id()).isEqualTo(user.getId());
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isEqualTo(NOW);
    }

    @Test
    void successfulAuthenticationAfterLockExpiryClearsTheLock() {
        for (int attempt = 0; attempt < 5; attempt++) user.recordFailedLogin(NOW);
        when(encoder.matches(anyString(), anyString())).thenReturn(true);
        Clock afterExpiry = Clock.fixed(NOW.plusSeconds(16 * 60), ZoneOffset.UTC);
        service = new AuthenticationService(users, credentials, encoder,
                new AuthenticationTransaction(users, credentials, externalIdentities, afterExpiry));

        service.authenticate("admin@glazr.local", "senha válida longa");

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isEqualTo(NOW.plusSeconds(16 * 60));
    }

    @Test
    void unknownEmailHasTheSameFailureType() {
        when(users.findByEmailIgnoreCase("unknown@glazr.local")).thenReturn(Optional.empty());
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate("unknown@glazr.local", "senha incorreta"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("E-mail ou senha inválidos");
    }

    @Test
    void rejectsAuthenticationIfIdentityChangesDuringPasswordComparison() {
        when(encoder.matches(anyString(), anyString())).thenAnswer(invocation -> {
            user.update("moved@glazr.local", null, null, NOW);
            return true;
        });

        assertThatThrownBy(() -> service.authenticate("admin@glazr.local", "senha válida longa"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("E-mail ou senha inválidos");
        assertThat(user.getLastLoginAt()).isNull();
    }
}
