package com.glazr.credential.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentityEntity, UUID> {
    Optional<ExternalIdentityEntity> findByProviderAndSubject(String provider, String subject);
    Optional<ExternalIdentityEntity> findByUserIdAndProvider(UUID userId, String provider);
    boolean existsByUserIdAndProvider(UUID userId, String provider);
}
