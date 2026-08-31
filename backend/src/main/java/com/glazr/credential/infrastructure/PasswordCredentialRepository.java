package com.glazr.credential.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordCredentialRepository extends JpaRepository<PasswordCredentialEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select credential from PasswordCredentialEntity credential where credential.userId = :userId")
    Optional<PasswordCredentialEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}
