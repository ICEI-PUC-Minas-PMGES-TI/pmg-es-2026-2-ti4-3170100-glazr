package com.glazr.user.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.authentication.infrastructure.UserSessionService;
import com.glazr.shared.exception.ConflictException;
import com.glazr.shared.exception.ResourceNotFoundException;
import com.glazr.user.api.CreateUserRequest;
import com.glazr.user.api.UpdateUserRequest;
import com.glazr.user.api.UserResponse;
import com.glazr.user.domain.EmailAddress;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.credential.infrastructure.ExternalIdentityEntity;
import com.glazr.credential.infrastructure.ExternalIdentityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import com.glazr.credential.application.TemporaryTokenService;
import com.glazr.invitation.application.InvitationDeliveryRequested;
import com.glazr.invitation.infrastructure.EmployeeInvitationEntity;
import com.glazr.invitation.infrastructure.EmployeeInvitationRepository;
import com.glazr.mail.application.EmployeeInvitationMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class UserManagementService {

    private final UserRepository users;
    private final AccessProfileRepository profiles;
    private final PasswordCredentialRepository passwordCredentials;
    private final ExternalIdentityRepository externalIdentities;
    private final EmployeeInvitationRepository invitations;
    private final TemporaryTokenService tokens;
    private final ApplicationEventPublisher events;
    private final AdministratorGuard administratorGuard;
    private final UserSessionService sessions;
    private final Clock clock;
    private final Duration invitationTtl;

    public UserManagementService(UserRepository users, AccessProfileRepository profiles,
                                 PasswordCredentialRepository passwordCredentials,
                                 ExternalIdentityRepository externalIdentities,
                                 EmployeeInvitationRepository invitations, TemporaryTokenService tokens,
                                 ApplicationEventPublisher events, AdministratorGuard administratorGuard,
                                 UserSessionService sessions, Clock clock,
                                 @Value("${glazr.invitation-ttl:48h}") Duration invitationTtl) {
        this.users = users;
        this.profiles = profiles;
        this.passwordCredentials = passwordCredentials;
        this.externalIdentities = externalIdentities;
        this.invitations = invitations;
        this.tokens = tokens;
        this.events = events;
        this.administratorGuard = administratorGuard;
        this.sessions = sessions;
        this.clock = clock;
        this.invitationTtl = invitationTtl;
    }

    @PreAuthorize("hasAuthority('USER_CREATE') and hasAuthority('ACCESS_PROFILE_ASSIGN')")
    @Transactional
    public UserResponse create(CreateUserRequest request, UUID createdByUserId) {
        String email = EmailAddress.normalize(request.email());
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("DUPLICATE_EMAIL", "Já existe um usuário com este e-mail");
        }
        Set<AccessProfileEntity> resolvedProfiles = resolveProfiles(request.accessProfileIds());
        var now = clock.instant();
        UserEntity user = UserEntity.createEmployee(email, request.displayName(), resolvedProfiles, now);
        users.saveAndFlush(user);
        var token = tokens.generate();
        EmployeeInvitationEntity invitation = invitations.save(EmployeeInvitationEntity.create(
                user.getId(), createdByUserId, token.hash(), now.plus(invitationTtl), now));
        events.publishEvent(new InvitationDeliveryRequested(invitation.getId(),
                new EmployeeInvitationMessage(user.getEmail(), user.getDisplayName(), token.rawValue(),
                        invitation.getExpiresAt())));
        return toResponse(user, invitation);
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @Transactional(readOnly = true)
    public Page<UserResponse> list(String query, Boolean active, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? "" : query.strip();
        return users.search(normalizedQuery, active, pageable).map(this::toResponse);
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return toResponse(requireUser(id));
    }

    @PreAuthorize("hasAuthority('USER_UPDATE') and (#request.accessProfileIds() == null or hasAuthority('ACCESS_PROFILE_ASSIGN'))")
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        UserEntity user = requireUser(id);
        Set<AccessProfileEntity> resolved = request.accessProfileIds() == null ? null : resolveProfiles(request.accessProfileIds());
        if (resolved != null) {
            boolean retainsAdmin = resolved.stream().anyMatch(p -> AccessProfileEntity.ADMINISTRATOR_ID.equals(p.getId()));
            administratorGuard.assertMayReplaceProfiles(user, retainsAdmin);
        }
        if (request.email() != null) {
            String normalized = EmailAddress.normalize(request.email());
            users.findByEmailIgnoreCase(normalized)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> { throw new ConflictException("DUPLICATE_EMAIL", "Já existe um usuário com este e-mail"); });
        }
        user.update(request.email(), request.displayName(), resolved, clock.instant());
        return toResponse(users.saveAndFlush(user));
    }

    @PreAuthorize("hasAuthority('USER_STATUS_UPDATE')")
    @Transactional
    public UserResponse updateStatus(UUID id, boolean active) {
        UserEntity user = requireUser(id);
        if (!active) administratorGuard.assertMayDeactivate(user);
        user.updateStatus(active, clock.instant());
        UserResponse response = toResponse(users.saveAndFlush(user));
        if (!active) sessions.deleteSessions(id);
        return response;
    }

    private UserEntity requireUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário"));
    }

    private UserResponse toResponse(UserEntity user) {
        return toResponse(user, invitations.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null));
    }

    private UserResponse toResponse(UserEntity user, EmployeeInvitationEntity invitation) {
        java.util.List<String> methods = new java.util.ArrayList<>();
        if (passwordCredentials.existsById(user.getId())) methods.add("PASSWORD");
        if (externalIdentities.existsByUserIdAndProvider(user.getId(), ExternalIdentityEntity.GOOGLE)) {
            methods.add("GOOGLE");
        }
        return UserResponse.from(user, methods,
                invitation == null ? null : invitation.statusAt(clock.instant()).name(),
                invitation == null ? null : invitation.getExpiresAt(),
                invitation == null ? null : invitation.getDeliveryStatus().name());
    }

    private Set<AccessProfileEntity> resolveProfiles(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Ao menos um perfil é obrigatório");
        Set<AccessProfileEntity> result = new HashSet<>(profiles.findAllById(ids));
        if (result.size() != ids.size() || result.stream().anyMatch(profile -> !profile.isActive())) {
            throw new IllegalArgumentException("Um ou mais perfis são inexistentes ou inativos");
        }
        return result;
    }
}
