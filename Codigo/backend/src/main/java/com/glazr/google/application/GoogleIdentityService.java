package com.glazr.google.application;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.authentication.application.AuthenticatedUserFactory;
import com.glazr.credential.domain.AuthenticationMethod;
import com.glazr.credential.infrastructure.ExternalIdentityEntity;
import com.glazr.credential.infrastructure.ExternalIdentityRepository;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.invitation.domain.InvitationStatus;
import com.glazr.invitation.infrastructure.EmployeeInvitationEntity;
import com.glazr.invitation.infrastructure.EmployeeInvitationRepository;
import com.glazr.shared.exception.ApplicationException;
import com.glazr.user.domain.AccountType;
import com.glazr.user.domain.EmailAddress;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumSet;
import java.util.UUID;

@Service
public class GoogleIdentityService {
    private final UserRepository users;
    private final ExternalIdentityRepository identities;
    private final PasswordCredentialRepository passwords;
    private final EmployeeInvitationRepository invitations;
    private final Clock clock;

    public GoogleIdentityService(UserRepository users, ExternalIdentityRepository identities,
                                 PasswordCredentialRepository passwords,
                                 EmployeeInvitationRepository invitations, Clock clock) {
        this.users = users;
        this.identities = identities;
        this.passwords = passwords;
        this.invitations = invitations;
        this.clock = clock;
    }

    @Transactional
    public AuthenticatedUser authenticate(String subject, String claimedEmail, boolean emailVerified,
                                          UUID invitationId, UUID expectedUserId) {
        if (!emailVerified || subject == null || subject.isBlank()) throw authenticationFailed();
        String email = EmailAddress.normalize(claimedEmail);
        var now = clock.instant();
        ExternalIdentityEntity identity = identities
                .findByProviderAndSubjectForUpdate(ExternalIdentityEntity.GOOGLE, subject).orElse(null);
        UserEntity user;
        if (identity != null) {
            user = requireEligibleLocked(identity.getUserId());
            if (!user.getEmail().equals(email)) throw emailMismatch();
            if (expectedUserId != null && !expectedUserId.equals(user.getId())) throw alreadyLinked();
        } else {
            user = resolveExpectedUser(email, invitationId, expectedUserId);
            ExternalIdentityEntity existingForUser = identities
                    .findByUserIdAndProviderForUpdate(user.getId(), ExternalIdentityEntity.GOOGLE).orElse(null);
            if (existingForUser != null && !existingForUser.getSubject().equals(subject)) throw alreadyLinked();
            if (existingForUser == null) {
                try {
                    identity = identities.saveAndFlush(
                            ExternalIdentityEntity.linkGoogle(user.getId(), subject, email, now));
                } catch (DataIntegrityViolationException exception) {
                    throw alreadyLinked();
                }
            } else {
                identity = existingForUser;
            }
        }
        identity.recordLogin(now);
        consumeInvitationIfPresent(user.getId(), invitationId, now);
        EnumSet<AuthenticationMethod> methods = EnumSet.of(AuthenticationMethod.GOOGLE);
        if (passwords.existsById(user.getId())) methods.add(AuthenticationMethod.PASSWORD);
        return AuthenticatedUserFactory.from(user, methods);
    }

    private UserEntity resolveExpectedUser(String email, UUID invitationId, UUID expectedUserId) {
        UUID targetId = expectedUserId;
        if (invitationId != null) {
            EmployeeInvitationEntity invitation = invitations.findByIdForUpdate(invitationId)
                    .orElseThrow(this::authenticationFailed);
            if (invitation.statusAt(clock.instant()) != InvitationStatus.PENDING) throw authenticationFailed();
            targetId = invitation.getUserId();
        }
        UserEntity user;
        if (targetId != null) {
            user = requireEligibleLocked(targetId);
        } else {
            UserEntity observed = users.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "GOOGLE_LOGIN_NOT_REGISTERED",
                            "Conta Google não cadastrada"));
            user = requireEligibleLocked(observed.getId());
        }
        if (!user.getEmail().equals(email)) throw emailMismatch();
        return user;
    }

    private void consumeInvitationIfPresent(UUID userId, UUID invitationId, java.time.Instant now) {
        if (invitationId != null) {
            EmployeeInvitationEntity invitation = invitations.findByIdForUpdate(invitationId)
                    .orElseThrow(this::authenticationFailed);
            if (!invitation.getUserId().equals(userId) || invitation.statusAt(now) != InvitationStatus.PENDING) {
                throw authenticationFailed();
            }
            invitation.consume(now);
        }
    }

    private UserEntity requireEligibleLocked(UUID id) {
        UserEntity user = users.findByIdForUpdate(id).orElseThrow(this::authenticationFailed);
        if (!user.isActive() || user.getAccountType() != AccountType.EMPLOYEE) throw authenticationFailed();
        return user;
    }

    private ApplicationException emailMismatch() {
        return error(HttpStatus.FORBIDDEN, "GOOGLE_EMAIL_MISMATCH", "O e-mail Google não corresponde à conta");
    }

    private ApplicationException alreadyLinked() {
        return error(HttpStatus.CONFLICT, "GOOGLE_IDENTITY_ALREADY_LINKED", "Identidade Google já vinculada");
    }

    private ApplicationException authenticationFailed() {
        return error(HttpStatus.UNAUTHORIZED, "GOOGLE_AUTHENTICATION_FAILED", "Autenticação Google recusada");
    }

    private ApplicationException error(HttpStatus status, String code, String message) {
        return new ApplicationException(status, code, message);
    }
}
