package com.glazr.inventory.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface GlassSheetRepository extends JpaRepository<GlassSheetEntity, UUID> {

    Page<GlassSheetEntity> findByGlassId(UUID glassId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(sheet.remainingAreaM2), 0) FROM GlassSheetEntity sheet WHERE sheet.glassId = :glassId")
    BigDecimal sumRemainingAreaByGlassId(@Param("glassId") UUID glassId);
}
