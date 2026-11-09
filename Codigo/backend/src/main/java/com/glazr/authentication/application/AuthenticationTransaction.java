package com.glazr.authentication.application;

import com.glazr.credential.domain.AuthenticationMethod;
import com.glazr.credential.infrastructure.ExternalIdentityEntity;
import com.glazr.credential.infrastructure.ExternalIdentityRepository;
import com.glazr.credential.infrastructure.PasswordCredentialRepository;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AuthenticationTransaction {

    private final UserRepository users;
    private final PasswordCredentialRepository credentials;
    private final ExternalIdentityRepository externalIdentities;
    private final Clock clock;

    public AuthenticationTransaction(UserRepository users, PasswordCredentialRepository credentials,
                                     ExternalIdentityRepository externalIdentities, Clock clock) {
        this.users = users;
        this.credentials = credentials;
        this.externalIdentities = externalIdentities;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthenticatedUser complete(UUID observedUserId, String observedEmail,
                                      String observedPasswordHash, boolean passwordMatches) {
        UserEntity user = users.findByIdForUpdate(observedUserId)
                .orElseThrow(AuthenticationFailedException::new);
        var credential = credentials.findByUserIdForUpdate(observedUserId)
                .orElseThrow(AuthenticationFailedException::new);
        if (!user.getEmail().equals(observedEmail)
                || !credential.getPasswordHash().equals(observedPasswordHash)) {
            throw new AuthenticationFailedException();
        }
        var now = clock.instant();
        if (!user.isActive() || user.isLocked(now)) throw new AuthenticationFailedException();
        if (!passwordMatches) {
            user.recordFailedLogin(now);
            users.save(user);
            throw new AuthenticationFailedException();
        }
        user.recordSuccessfulLogin(now);
        users.save(user);
        var methods = java.util.EnumSet.of(AuthenticationMethod.PASSWORD);
        if (externalIdentities.existsByUserIdAndProvider(user.getId(), ExternalIdentityEntity.GOOGLE)) {
            methods.add(AuthenticationMethod.GOOGLE);
        }
        return AuthenticatedUserFactory.from(user, methods);
    }
}
