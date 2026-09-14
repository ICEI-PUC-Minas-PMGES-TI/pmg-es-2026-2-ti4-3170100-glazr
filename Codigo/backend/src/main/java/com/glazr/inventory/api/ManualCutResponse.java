package com.glazr.inventory.api;

import com.glazr.inventory.infrastructure.GlassSheetEntity;
import com.glazr.inventory.infrastructure.StockMovementEntity;

import java.math.BigDecimal;

public record ManualCutResponse(
        GlassSheetResponse sheet,
        StockMovementResponse movement,
        BigDecimal yieldPercent
) {
    public static ManualCutResponse from(GlassSheetEntity sheet, StockMovementEntity movement, BigDecimal yieldPercent) {
        return new ManualCutResponse(GlassSheetResponse.from(sheet), StockMovementResponse.from(movement), yieldPercent);
    }
}
