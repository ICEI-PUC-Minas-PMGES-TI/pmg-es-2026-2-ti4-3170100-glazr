package com.glazr.password.application;

import com.glazr.authentication.infrastructure.UserSessionService;
import com.glazr.credential.application.TemporaryTokenService;
import com.glazr.credential.infrastructure.PasswordCredentialEntity;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.credential.infrastructure.PasswordResetEntity;
import com.glazr.credential.infrastructure.PasswordResetRepository;
import com.glazr.mail.application.PasswordResetMessage;
import com.glazr.shared.exception.ApplicationException;
import com.glazr.user.domain.AccountType;
import com.glazr.user.domain.EmailAddress;
import com.glazr.user.domain.PasswordPolicy;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
public class PasswordService {
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(2);
    private final UserRepository users;
    private final PasswordCredentialRepository credentials;
    private final PasswordResetRepository resets;
    private final TemporaryTokenService tokens;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final UserSessionService sessions;
    private final Clock clock;
    private final Duration resetTtl;

    public PasswordService(UserRepository users, PasswordCredentialRepository credentials,
                           PasswordResetRepository resets, TemporaryTokenService tokens,
                           PasswordEncoder encoder, ApplicationEventPublisher events,
                           UserSessionService sessions, Clock clock,
                           @Value("${glazr.password-reset-ttl:30m}") Duration resetTtl) {
        this.users = users;
        this.credentials = credentials;
        this.resets = resets;
        this.tokens = tokens;
        this.encoder = encoder;
        this.events = events;
        this.sessions = sessions;
        this.clock = clock;
        this.resetTtl = resetTtl;
    }

    @Transactional
    public void change(UUID userId, String currentPassword, String newPassword) {
        PasswordPolicy.validate(newPassword);
        PasswordCredentialEntity credential = credentials.findByUserIdForUpdate(userId)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "PASSWORD_CREDENTIAL_NOT_CONFIGURED",
                        "A conta não possui senha local"));
        if (!encoder.matches(currentPassword, credential.getPasswordHash())) {
            throw error(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_INVALID", "A senha atual é inválida");
        }
        if (encoder.matches(newPassword, credential.getPasswordHash())) {
            throw error(HttpStatus.BAD_REQUEST, "PASSWORD_REUSE_NOT_ALLOWED", "A nova senha deve ser diferente");
        }
        credential.changePassword(encoder.encode(newPassword), clock.instant());
    }

    @Transactional
    public void requestReset(String submittedEmail) {
        String email;
        try {
            email = EmailAddress.normalize(submittedEmail);
        } catch (RuntimeException exception) {
            return;
        }
        UserEntity user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isActive() || user.getAccountType() != AccountType.EMPLOYEE) return;
        var now = clock.instant();
        PasswordResetEntity latest = resets.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        if (latest != null && latest.getCreatedAt().plus(REQUEST_COOLDOWN).isAfter(now)) return;
        resets.findUsableByUserIdForUpdate(user.getId()).ifPresent(reset -> reset.revoke(now));
        var token = tokens.generate();
        PasswordResetEntity reset = resets.save(PasswordResetEntity.create(
                user.getId(), token.hash(), now.plus(resetTtl), now));
        events.publishEvent(new PasswordResetDeliveryRequested(new PasswordResetMessage(
                user.getEmail(), user.getDisplayName(), token.rawValue(), reset.getExpiresAt())));
    }

    @Transactional
    public UUID completeReset(String rawToken, String newPassword) {
        PasswordPolicy.validate(newPassword);
        PasswordResetEntity reset = resets.findByTokenHashForUpdate(tokens.hash(rawToken))
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_INVALID", "Redefinição inválida"));
        var now = clock.instant();
        if (reset.getConsumedAt() != null || reset.getRevokedAt() != null) {
            throw error(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_INVALID", "Redefinição inválida");
        }
        if (!reset.getExpiresAt().isAfter(now)) {
            throw error(HttpStatus.GONE, "PASSWORD_RESET_EXPIRED", "A redefinição expirou");
        }
        UserEntity user = users.findByIdForUpdate(reset.getUserId())
                .filter(candidate -> candidate.isActive() && candidate.getAccountType() == AccountType.EMPLOYEE)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_INVALID", "Redefinição inválida"));
        String encoded = encoder.encode(newPassword);
        credentials.findByUserIdForUpdate(user.getId()).ifPresentOrElse(
                credential -> credential.changePassword(encoded, now),
                () -> credentials.save(PasswordCredentialEntity.create(user.getId(), encoded, now)));
        reset.consume(now);
        resets.findUsableByUserIdForUpdate(user.getId())
                .filter(other -> !other.getId().equals(reset.getId())).ifPresent(other -> other.revoke(now));
        return user.getId();
    }

    public void invalidateAllSessions(UUID userId) { sessions.deleteSessions(userId); }

    public void invalidateOtherSessions(UUID userId, String currentSessionId) {
        sessions.deleteOtherSessions(userId, currentSessionId);
    }

    private ApplicationException error(HttpStatus status, String code, String message) {
        return new ApplicationException(status, code, message);
    }
}
