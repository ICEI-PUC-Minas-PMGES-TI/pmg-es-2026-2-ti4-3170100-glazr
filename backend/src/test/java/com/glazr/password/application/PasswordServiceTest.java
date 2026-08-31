package com.glazr.password.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.authentication.infrastructure.UserSessionService;
import com.glazr.credential.application.TemporaryTokenService;
import com.glazr.credential.infrastructure.PasswordCredentialEntity;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.credential.infrastructure.PasswordResetEntity;
import com.glazr.credential.infrastructure.PasswordResetRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordServiceTest {
    @Test
    void resetCreatesTheFirstPasswordAndConsumesTheToken() {
        Instant now = Instant.parse("2026-08-31T12:00:00Z");
        TemporaryTokenService tokens = new TemporaryTokenService();
        var token = tokens.generate();
        UserEntity user = UserEntity.createEmployee("person@example.com", "Person",
                Set.of(AccessProfileEntity.create("Operador", null, Set.of(), now)), now);
        PasswordResetEntity reset = PasswordResetEntity.create(user.getId(), token.hash(), now.plusSeconds(1800), now);
        UserRepository users = mock(UserRepository.class);
        PasswordCredentialRepository credentials = mock(PasswordCredentialRepository.class);
        PasswordResetRepository resets = mock(PasswordResetRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(resets.findByTokenHashForUpdate(token.hash())).thenReturn(Optional.of(reset));
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(credentials.findByUserIdForUpdate(user.getId())).thenReturn(Optional.empty());
        when(encoder.encode("uma frase senha segura")).thenReturn("hash-seguro");
        PasswordService service = new PasswordService(users, credentials, resets, tokens, encoder,
                mock(ApplicationEventPublisher.class), mock(UserSessionService.class),
                Clock.fixed(now, ZoneOffset.UTC), Duration.ofMinutes(30));

        var userId = service.completeReset(token.rawValue(), "uma frase senha segura");

        assertThat(userId).isEqualTo(user.getId());
        assertThat(reset.getConsumedAt()).isEqualTo(now);
        verify(credentials).save(any(PasswordCredentialEntity.class));
    }
}
