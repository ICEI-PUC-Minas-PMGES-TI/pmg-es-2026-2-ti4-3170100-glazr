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
import com.glazr.user.domain.PasswordPolicy;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class UserManagementService {

    private final UserRepository users;
    private final AccessProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final AdministratorGuard administratorGuard;
    private final UserSessionService sessions;
    private final Clock clock;

    public UserManagementService(UserRepository users, AccessProfileRepository profiles,
                                 PasswordEncoder passwordEncoder, AdministratorGuard administratorGuard,
                                 UserSessionService sessions, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.administratorGuard = administratorGuard;
        this.sessions = sessions;
        this.clock = clock;
    }

    @PreAuthorize("hasAuthority('USER_CREATE') and hasAuthority('ACCESS_PROFILE_ASSIGN')")
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = EmailAddress.normalize(request.email());
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("DUPLICATE_EMAIL", "Já existe um usuário com este e-mail");
        }
        PasswordPolicy.validate(request.password());
        Set<AccessProfileEntity> resolvedProfiles = resolveProfiles(request.accessProfileIds());
        UserEntity user = UserEntity.create(email, request.displayName(), passwordEncoder.encode(request.password()),
                resolvedProfiles, clock.instant());
        return UserResponse.from(users.saveAndFlush(user));
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @Transactional(readOnly = true)
    public Page<UserResponse> list(String query, Boolean active, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.strip();
        return users.search(normalizedQuery, active, pageable).map(UserResponse::from);
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(requireUser(id));
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
        return UserResponse.from(users.saveAndFlush(user));
    }

    @PreAuthorize("hasAuthority('USER_STATUS_UPDATE')")
    @Transactional
    public UserResponse updateStatus(UUID id, boolean active) {
        UserEntity user = requireUser(id);
        if (!active) administratorGuard.assertMayDeactivate(user);
        user.updateStatus(active, clock.instant());
        UserResponse response = UserResponse.from(users.saveAndFlush(user));
        if (!active) sessions.deleteSessions(id);
        return response;
    }

    private UserEntity requireUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário"));
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
