package com.glazr.authentication.application;

import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationTransaction {

    private final UserRepository users;
    private final Clock clock;

    public AuthenticationTransaction(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthenticatedUser complete(UUID observedUserId, String observedEmail,
                                      String observedPasswordHash, boolean passwordMatches) {
        UserEntity user = users.findByIdForUpdate(observedUserId)
                .orElseThrow(AuthenticationFailedException::new);
        if (!user.getEmail().equals(observedEmail)
                || !user.getPasswordHash().equals(observedPasswordHash)) {
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
        return AuthenticatedUserFactory.from(user);
    }
}
