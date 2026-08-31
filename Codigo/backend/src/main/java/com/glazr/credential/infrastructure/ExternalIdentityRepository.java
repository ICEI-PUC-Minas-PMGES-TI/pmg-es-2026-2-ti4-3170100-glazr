package com.glazr.credential.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentityEntity, UUID> {
    Optional<ExternalIdentityEntity> findByProviderAndSubject(String provider, String subject);
    Optional<ExternalIdentityEntity> findByUserIdAndProvider(UUID userId, String provider);
    boolean existsByUserIdAndProvider(UUID userId, String provider);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select identity from ExternalIdentityEntity identity where identity.provider = :provider and identity.subject = :subject")
    Optional<ExternalIdentityEntity> findByProviderAndSubjectForUpdate(@Param("provider") String provider,
                                                                       @Param("subject") String subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select identity from ExternalIdentityEntity identity where identity.userId = :userId and identity.provider = :provider")
    Optional<ExternalIdentityEntity> findByUserIdAndProviderForUpdate(@Param("userId") UUID userId,
                                                                      @Param("provider") String provider);
}
