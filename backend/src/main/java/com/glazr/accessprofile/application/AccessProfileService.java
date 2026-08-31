package com.glazr.accessprofile.application;

import com.glazr.accessprofile.api.AccessProfileRequest;
import com.glazr.accessprofile.api.AccessProfileResponse;
import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.permission.infrastructure.PermissionEntity;
import com.glazr.permission.infrastructure.PermissionRepository;
import com.glazr.shared.exception.ConflictException;
import com.glazr.shared.exception.ResourceNotFoundException;
import com.glazr.user.infrastructure.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AccessProfileService {

    private final AccessProfileRepository profiles;
    private final PermissionRepository permissions;
    private final UserRepository users;
    private final Clock clock;

    public AccessProfileService(AccessProfileRepository profiles, PermissionRepository permissions,
                                UserRepository users, Clock clock) {
        this.profiles = profiles;
        this.permissions = permissions;
        this.users = users;
        this.clock = clock;
    }

    @PreAuthorize("hasAuthority('ACCESS_PROFILE_CREATE')")
    @Transactional
    public AccessProfileResponse create(AccessProfileRequest request) {
        rejectDuplicateName(request.name(), null);
        Set<PermissionEntity> resolvedPermissions = resolvePermissions(request.permissionKeys());
        AccessProfileEntity profile = AccessProfileEntity.create(request.name(), request.description(),
                resolvedPermissions, clock.instant());
        if (!request.active()) profile.replace(request.name(), request.description(), false,
                resolvedPermissions, clock.instant());
        return AccessProfileResponse.from(profiles.saveAndFlush(profile));
    }

    @PreAuthorize("hasAuthority('ACCESS_PROFILE_READ')")
    @Transactional(readOnly = true)
    public Page<AccessProfileResponse> list(String query, Pageable pageable) {
        Page<AccessProfileEntity> result = query == null || query.isBlank()
                ? profiles.findAll(pageable)
                : profiles.findByNameContainingIgnoreCase(query.strip(), pageable);
        return result.map(AccessProfileResponse::from);
    }

    @PreAuthorize("hasAuthority('ACCESS_PROFILE_READ')")
    @Transactional(readOnly = true)
    public AccessProfileResponse get(UUID id) { return AccessProfileResponse.from(requireProfile(id)); }

    @PreAuthorize("hasAuthority('ACCESS_PROFILE_UPDATE')")
    @Transactional
    public AccessProfileResponse replace(UUID id, AccessProfileRequest request) {
        AccessProfileEntity profile = requireProfile(id);
        if (profile.isSystemProfile()) throw protectedProfile();
        rejectDuplicateName(request.name(), id);
        profile.replace(request.name(), request.description(), request.active(),
                resolvePermissions(request.permissionKeys()), clock.instant());
        return AccessProfileResponse.from(profiles.saveAndFlush(profile));
    }

    @PreAuthorize("hasAuthority('ACCESS_PROFILE_DELETE')")
    @Transactional
    public void delete(UUID id) {
        AccessProfileEntity profile = requireProfile(id);
        if (profile.isSystemProfile()) throw protectedProfile();
        if (users.countByAccessProfilesId(id) > 0) {
            throw new ConflictException("ACCESS_PROFILE_IN_USE", "O perfil está associado a usuários");
        }
        profiles.delete(profile);
        profiles.flush();
    }

    private AccessProfileEntity requireProfile(UUID id) {
        return profiles.findById(id).orElseThrow(() -> new ResourceNotFoundException("Perfil de acesso"));
    }

    private Set<PermissionEntity> resolvePermissions(Set<String> keys) {
        if (keys == null || keys.isEmpty()) throw new IllegalArgumentException("Ao menos uma permissão é obrigatória");
        Set<String> normalized = keys.stream().map(key -> key.strip().toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        Set<PermissionEntity> found = new HashSet<>(permissions.findAllById(normalized));
        if (found.size() != normalized.size()) throw new IllegalArgumentException("Uma ou mais permissões são desconhecidas");
        return found;
    }

    private void rejectDuplicateName(String name, UUID currentId) {
        profiles.findByNameIgnoreCase(name.strip()).filter(profile -> !profile.getId().equals(currentId))
                .ifPresent(profile -> { throw new ConflictException("DUPLICATE_ACCESS_PROFILE", "Já existe um perfil com este nome"); });
    }

    private ConflictException protectedProfile() {
        return new ConflictException("PROTECTED_ACCESS_PROFILE", "O perfil ADMINISTRADOR é protegido");
    }
}
