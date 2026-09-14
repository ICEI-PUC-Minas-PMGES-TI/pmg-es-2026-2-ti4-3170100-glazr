package com.glazr.inventory.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovementEntity, UUID> {

    @Query("""
            SELECT movement FROM StockMovementEntity movement
            JOIN GlassSheetEntity sheet ON sheet.id = movement.glassSheetId
            WHERE sheet.glassId = :glassId
            """)
    Page<StockMovementEntity> findByGlassId(@Param("glassId") UUID glassId, Pageable pageable);
}
