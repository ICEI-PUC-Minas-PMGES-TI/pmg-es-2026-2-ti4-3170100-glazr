package com.glazr.inventory.api;

import com.glazr.glass.infrastructure.GlassEntity;
import com.glazr.inventory.domain.StockStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record GlassStockSummaryResponse(
        UUID glassId,
        String glassName,
        BigDecimal thicknessMm,
        String finish,
        String color,
        boolean glassActive,
        BigDecimal availableAreaM2,
        BigDecimal minimumAreaM2,
        StockStatus status
) {
    public static GlassStockSummaryResponse from(
            GlassEntity glass, BigDecimal availableAreaM2, BigDecimal minimumAreaM2, StockStatus status) {
        return new GlassStockSummaryResponse(
                glass.getId(), glass.getName(), glass.getThicknessMm(), glass.getFinish(), glass.getColor(),
                glass.isActive(), availableAreaM2, minimumAreaM2, status
        );
    }
}
