package com.glazr.credential.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRepository extends JpaRepository<PasswordResetEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reset from PasswordResetEntity reset where reset.tokenHash = :tokenHash")
    Optional<PasswordResetEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reset from PasswordResetEntity reset where reset.userId = :userId
              and reset.consumedAt is null and reset.revokedAt is null
            """)
    Optional<PasswordResetEntity> findUsableByUserIdForUpdate(@Param("userId") UUID userId);
}
