package com.glazr.inventory.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface GlassSheetRepository extends JpaRepository<GlassSheetEntity, UUID> {

    Page<GlassSheetEntity> findByGlassId(UUID glassId, Pageable pageable);

    boolean existsByGlassId(UUID glassId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sheet FROM GlassSheetEntity sheet WHERE sheet.id = :id")
    Optional<GlassSheetEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT COALESCE(SUM(sheet.remainingAreaM2), 0) FROM GlassSheetEntity sheet WHERE sheet.glassId = :glassId")
    BigDecimal sumRemainingAreaByGlassId(@Param("glassId") UUID glassId);
}
