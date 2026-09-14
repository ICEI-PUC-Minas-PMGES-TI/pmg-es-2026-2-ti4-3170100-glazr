package com.glazr.kit.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KitRepository extends JpaRepository<KitEntity, UUID> {
    Optional<KitEntity> findByNameIgnoreCase(String name);

    Page<KitEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}