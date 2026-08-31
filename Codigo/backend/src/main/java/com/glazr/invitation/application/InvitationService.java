package com.glazr.invitation.application;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.authentication.application.AuthenticatedUserFactory;
import com.glazr.credential.application.TemporaryTokenService;
import com.glazr.credential.domain.AuthenticationMethod;
import com.glazr.credential.infrastructure.ExternalIdentityEntity;
import com.glazr.credential.infrastructure.ExternalIdentityRepository;
import com.glazr.credential.infrastructure.PasswordCredentialEntity;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.invitation.api.InvitationInspectionResponse;
import com.glazr.invitation.api.InvitationResponse;
import com.glazr.invitation.domain.InvitationStatus;
import com.glazr.invitation.infrastructure.EmployeeInvitationEntity;
import com.glazr.invitation.infrastructure.EmployeeInvitationRepository;
import com.glazr.mail.application.EmployeeInvitationMessage;
import com.glazr.shared.exception.ConflictException;
import com.glazr.shared.exception.ResourceNotFoundException;
import com.glazr.user.domain.AccountType;
import com.glazr.user.domain.PasswordPolicy;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {
    private final EmployeeInvitationRepository invitations;
    private final UserRepository users;
    private final PasswordCredentialRepository passwords;
    private final ExternalIdentityRepository externalIdentities;
    private final TemporaryTokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Duration ttl;
    private final boolean googleEnabled;

    public InvitationService(EmployeeInvitationRepository invitations, UserRepository users,
                             PasswordCredentialRepository passwords, ExternalIdentityRepository externalIdentities,
                             TemporaryTokenService tokens, PasswordEncoder passwordEncoder,
                             ApplicationEventPublisher events, Clock clock,
                             @Value("${glazr.invitation-ttl:48h}") Duration ttl,
                             @Value("${glazr.google-auth-enabled:false}") boolean googleEnabled) {
        this.invitations = invitations;
        this.users = users;
        this.passwords = passwords;
        this.externalIdentities = externalIdentities;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.clock = clock;
        this.ttl = ttl;
        this.googleEnabled = googleEnabled;
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @Transactional(readOnly = true)
    public InvitationResponse getForUser(UUID userId) {
        requireUser(userId);
        EmployeeInvitationEntity invitation = invitations.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite"));
        return InvitationResponse.from(invitation, clock.instant());
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Transactional
    public InvitationResponse resend(UUID userId, UUID createdByUserId) {
        UserEntity user = requireUser(userId);
        ensureNotActivated(userId);
        var now = clock.instant();
        invitations.findUsableByUserIdForUpdate(userId).ifPresent(current -> current.revoke(now));
        var token = tokens.generate();
        EmployeeInvitationEntity replacement = invitations.save(EmployeeInvitationEntity.create(
                userId, createdByUserId, token.hash(), now.plus(ttl), now));
        events.publishEvent(new InvitationDeliveryRequested(replacement.getId(),
                new EmployeeInvitationMessage(user.getEmail(), user.getDisplayName(), token.rawValue(),
                        replacement.getExpiresAt())));
        return InvitationResponse.from(replacement, now);
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Transactional
    public InvitationResponse revoke(UUID userId) {
        requireUser(userId);
        EmployeeInvitationEntity invitation = invitations.findUsableByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite ativo"));
        invitation.revoke(clock.instant());
        return InvitationResponse.from(invitation, clock.instant());
    }

    @Transactional(readOnly = true)
    public InvitationInspectionResponse inspect(String rawToken) {
        EmployeeInvitationEntity invitation = invitations.findByTokenHash(tokens.hash(rawToken))
                .orElseThrow(this::invalid);
        validateUsable(invitation);
        UserEntity user = requireEligibleUser(invitation.getUserId());
        List<String> methods = new ArrayList<>(List.of("PASSWORD"));
        if (googleEnabled) methods.add("GOOGLE");
        return new InvitationInspectionResponse(maskName(user.getDisplayName()), maskEmail(user.getEmail()),
                invitation.getExpiresAt(), List.copyOf(methods));
    }

    @Transactional(readOnly = true)
    public UUID prepareGoogle(String rawToken) {
        if (!googleEnabled) throw new InvitationException(HttpStatus.NOT_FOUND, "GOOGLE_AUTH_DISABLED",
                "Autenticação Google não está habilitada");
        EmployeeInvitationEntity invitation = invitations.findByTokenHash(tokens.hash(rawToken))
                .orElseThrow(this::invalid);
        validateUsable(invitation);
        requireEligibleUser(invitation.getUserId());
        return invitation.getId();
    }

    @Transactional
    public AuthenticatedUser acceptWithPassword(String rawToken, String password) {
        PasswordPolicy.validate(password);
        EmployeeInvitationEntity invitation = invitations.findByTokenHashForUpdate(tokens.hash(rawToken))
                .orElseThrow(this::invalid);
        validateUsable(invitation);
        UserEntity user = requireEligibleUser(invitation.getUserId());
        if (passwords.existsById(user.getId())) {
            throw new ConflictException("PASSWORD_CREDENTIAL_ALREADY_CONFIGURED", "A senha já foi configurada");
        }
        var now = clock.instant();
        passwords.save(PasswordCredentialEntity.create(user.getId(), passwordEncoder.encode(password), now));
        invitation.consume(now);
        return AuthenticatedUserFactory.from(user, authenticationMethods(user.getId(), true));
    }

    private void ensureNotActivated(UUID userId) {
        if (passwords.existsById(userId)
                || externalIdentities.existsByUserIdAndProvider(userId, ExternalIdentityEntity.GOOGLE)) {
            throw new ConflictException("INVITATION_ALREADY_ACTIVATED", "O usuário já possui um método de acesso");
        }
    }

    private UserEntity requireEligibleUser(UUID userId) {
        UserEntity user = requireUser(userId);
        if (!user.isActive() || user.getAccountType() != AccountType.EMPLOYEE) throw invalid();
        return user;
    }

    private UserEntity requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário"));
    }

    private void validateUsable(EmployeeInvitationEntity invitation) {
        InvitationStatus status = invitation.statusAt(clock.instant());
        if (status == InvitationStatus.EXPIRED) {
            throw new InvitationException(HttpStatus.GONE, "INVITATION_EXPIRED", "O convite expirou");
        }
        if (status == InvitationStatus.REVOKED) {
            throw new InvitationException(HttpStatus.GONE, "INVITATION_REVOKED", "O convite foi revogado");
        }
        if (status == InvitationStatus.CONSUMED) {
            throw new InvitationException(HttpStatus.CONFLICT, "INVITATION_ALREADY_USED", "O convite já foi utilizado");
        }
    }

    private InvitationException invalid() {
        return new InvitationException(HttpStatus.BAD_REQUEST, "INVITATION_INVALID", "Convite inválido");
    }

    private List<AuthenticationMethod> authenticationMethods(UUID userId, boolean passwordNowPresent) {
        List<AuthenticationMethod> methods = new ArrayList<>();
        if (passwordNowPresent || passwords.existsById(userId)) methods.add(AuthenticationMethod.PASSWORD);
        if (externalIdentities.existsByUserIdAndProvider(userId, ExternalIdentityEntity.GOOGLE)) {
            methods.add(AuthenticationMethod.GOOGLE);
        }
        return methods;
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String visible = local.substring(0, 1);
        return visible + "***" + email.substring(at);
    }

    private String maskName(String name) {
        String stripped = name.strip();
        return stripped.substring(0, 1) + "***";
    }

}
