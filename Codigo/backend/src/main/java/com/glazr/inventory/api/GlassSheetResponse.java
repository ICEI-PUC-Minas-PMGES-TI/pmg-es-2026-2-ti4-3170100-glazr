package com.glazr.inventory.api;

import com.glazr.inventory.domain.SheetStatus;
import com.glazr.inventory.infrastructure.GlassSheetEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GlassSheetResponse(
        UUID id,
        UUID glassId,
        BigDecimal originalWidthMm,
        BigDecimal originalHeightMm,
        BigDecimal originalAreaM2,
        BigDecimal remainingAreaM2,
        SheetStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static GlassSheetResponse from(GlassSheetEntity sheet) {
        return new GlassSheetResponse(
                sheet.getId(), sheet.getGlassId(), sheet.getOriginalWidthMm(), sheet.getOriginalHeightMm(),
                sheet.getOriginalAreaM2(), sheet.getRemainingAreaM2(), sheet.getStatus(),
                sheet.getCreatedAt(), sheet.getUpdatedAt()
        );
    }
}
