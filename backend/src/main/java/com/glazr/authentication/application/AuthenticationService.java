package com.glazr.authentication.application;

import com.glazr.user.domain.EmailAddress;
import com.glazr.user.infrastructure.UserEntity;
import com.glazr.user.infrastructure.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthenticationService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationTransaction transaction;
    private final String dummyPasswordHash;

    public AuthenticationService(UserRepository users, PasswordEncoder passwordEncoder,
                                 AuthenticationTransaction transaction) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.transaction = transaction;
        this.dummyPasswordHash = passwordEncoder.encode("glazr authentication timing sentinel");
    }

    public AuthenticatedUser authenticate(String email, String password) {
        String normalizedEmail;
        try {
            normalizedEmail = EmailAddress.normalize(email);
        } catch (RuntimeException exception) {
            passwordEncoder.matches(password == null ? "" : password, dummyPasswordHash);
            throw new AuthenticationFailedException();
        }

        Optional<UserEntity> candidate = users.findByEmailIgnoreCase(normalizedEmail);
        String hash = candidate.map(UserEntity::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(password == null ? "" : password, hash);
        if (candidate.isEmpty()) throw new AuthenticationFailedException();
        UserEntity observed = candidate.get();
        return transaction.complete(observed.getId(), normalizedEmail, hash, passwordMatches);
    }
}
