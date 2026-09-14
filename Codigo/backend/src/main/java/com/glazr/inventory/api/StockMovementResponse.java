package com.glazr.inventory.api;

import com.glazr.inventory.domain.StockMovementType;
import com.glazr.inventory.infrastructure.StockMovementEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID glassSheetId,
        StockMovementType movementType,
        BigDecimal previousRemainingAreaM2,
        BigDecimal resultingRemainingAreaM2,
        BigDecimal pieceWidthMm,
        BigDecimal pieceHeightMm,
        BigDecimal pieceAreaM2,
        BigDecimal lossAreaM2,
        String notes,
        UUID performedByUserId,
        Instant createdAt
) {
    public static StockMovementResponse from(StockMovementEntity movement) {
        return new StockMovementResponse(
                movement.getId(), movement.getGlassSheetId(), movement.getMovementType(),
                movement.getPreviousRemainingAreaM2(), movement.getResultingRemainingAreaM2(),
                movement.getPieceWidthMm(), movement.getPieceHeightMm(), movement.getPieceAreaM2(),
                movement.getLossAreaM2(), movement.getNotes(), movement.getPerformedByUserId(), movement.getCreatedAt()
        );
    }
}
