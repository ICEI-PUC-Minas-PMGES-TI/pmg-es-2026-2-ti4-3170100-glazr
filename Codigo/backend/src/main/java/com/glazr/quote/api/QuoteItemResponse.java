package com.glazr.quote.api;

import com.glazr.quote.infrastructure.QuoteGlassItemEntity;
import com.glazr.quote.infrastructure.QuoteKitItemEntity;

import java.math.BigDecimal;
import java.util.UUID;

public final class QuoteItemResponse {

    public record Glass(
            UUID id,
            UUID glassId,
            String glassName,
            BigDecimal widthMm,
            BigDecimal heightMm,
            int quantity,
            BigDecimal unitAreaM2,
            BigDecimal totalAreaM2,
            BigDecimal pricePerSquareMeter,
            BigDecimal totalPrice,
            String notes
    ) {
        public static Glass from(QuoteGlassItemEntity item, String glassName) {
            return new Glass(
                    item.getId(),
                    item.getGlassId(),
                    glassName,
                    item.getWidthMm(),
                    item.getHeightMm(),
                    item.getQuantity(),
                    item.getUnitAreaM2(),
                    item.getTotalAreaM2(),
                    item.getPricePerSquareMeter(),
                    item.getTotalPrice(),
                    item.getNotes()
            );
        }
    }

    public record Kit(
            UUID id,
            UUID kitId,
            String kitName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String notes
    ) {
        public static Kit from(QuoteKitItemEntity item, String kitName) {
            return new Kit(
                    item.getId(),
                    item.getKitId(),
                    kitName,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice(),
                    item.getNotes()
            );
        }
    }
}
