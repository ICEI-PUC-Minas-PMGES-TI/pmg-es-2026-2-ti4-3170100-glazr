package com.glazr.accessprofile.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface AccessProfileRepository extends JpaRepository<AccessProfileEntity, UUID> {
    Optional<AccessProfileEntity> findByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AccessProfileEntity p where p.id = :id")
    Optional<AccessProfileEntity> findByIdForUpdate(@Param("id") UUID id);

    Page<AccessProfileEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
