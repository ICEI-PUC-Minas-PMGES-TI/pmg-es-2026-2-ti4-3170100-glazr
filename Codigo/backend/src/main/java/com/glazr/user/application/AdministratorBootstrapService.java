package com.glazr.user.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.configuration.BootstrapCredentials;
import com.glazr.permission.infrastructure.PermissionRepository;
import com.glazr.user.domain.PasswordPolicy;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import com.glazr.credential.infrastructure.PasswordCredentialEntity;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class AdministratorBootstrapService {

    public enum Outcome { CREATED, SKIPPED_EXISTING_USERS }

    private final UserRepository users;
    private final AccessProfileRepository profiles;
    private final PermissionRepository permissions;
    private final PasswordEncoder passwordEncoder;
    private final PasswordCredentialRepository passwordCredentials;
    private final Clock clock;

    public AdministratorBootstrapService(UserRepository users, AccessProfileRepository profiles,
                                         PermissionRepository permissions, PasswordEncoder passwordEncoder,
                                         PasswordCredentialRepository passwordCredentials, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.permissions = permissions;
        this.passwordEncoder = passwordEncoder;
        this.passwordCredentials = passwordCredentials;
        this.clock = clock;
    }

    @Transactional
    public Outcome bootstrap(Supplier<BootstrapCredentials> credentialsSupplier) {
        AccessProfileEntity administrator = profiles.findByIdForUpdate(AccessProfileEntity.ADMINISTRATOR_ID)
                .orElseThrow(() -> new IllegalStateException("Perfil ADMINISTRADOR não foi criado pela migration"));
        if (users.count() > 0) return Outcome.SKIPPED_EXISTING_USERS;
        BootstrapCredentials credentials = credentialsSupplier.get();
        credentials.validateComplete();
        PasswordPolicy.validate(credentials.password());
        if (administrator.getPermissions().size() != permissions.count()) {
            throw new IllegalStateException("Perfil ADMINISTRADOR não contém todo o catálogo de permissões");
        }
        var now = clock.instant();
        UserEntity user = UserEntity.createEmployee(
                credentials.email(), credentials.name(), Set.of(administrator), now);
        users.saveAndFlush(user);
        passwordCredentials.save(PasswordCredentialEntity.create(
                user.getId(), passwordEncoder.encode(credentials.password()), now));
        return Outcome.CREATED;
    }
}
