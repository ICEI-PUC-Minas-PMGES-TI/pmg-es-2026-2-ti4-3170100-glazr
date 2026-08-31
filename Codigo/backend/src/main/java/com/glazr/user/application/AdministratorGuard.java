package com.glazr.user.application;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.accessprofile.infrastructure.AccessProfileRepository;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class AdministratorGuard {

    private final UserRepository users;
    private final AccessProfileRepository profiles;

    public AdministratorGuard(UserRepository users, AccessProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    public void assertMayDeactivate(UserEntity user) {
        boolean administrator = user.getAccessProfiles().stream()
                .anyMatch(profile -> AccessProfileEntity.ADMINISTRATOR_ID.equals(profile.getId()));
        if (!user.isActive() || !administrator) return;
        profiles.findByIdForUpdate(AccessProfileEntity.ADMINISTRATOR_ID)
                .orElseThrow(() -> new IllegalStateException("Perfil ADMINISTRADOR não encontrado"));
        if (users.countActiveByProfileId(AccessProfileEntity.ADMINISTRATOR_ID) <= 1) {
            throw new LastActiveAdministratorException();
        }
    }

    public void assertMayReplaceProfiles(UserEntity user, boolean retainsAdministratorProfile) {
        if (!retainsAdministratorProfile) assertMayDeactivate(user);
    }
}
