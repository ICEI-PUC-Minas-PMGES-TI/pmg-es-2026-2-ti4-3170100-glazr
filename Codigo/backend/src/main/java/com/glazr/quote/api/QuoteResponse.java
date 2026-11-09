package com.glazr.quote.api;

import com.glazr.quote.domain.QuoteStatus;
import com.glazr.quote.infrastructure.QuoteEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        Long quoteNumber,
        String customerName,
        String customerPhone,
        String customerEmail,
        String customerAddress,
        QuoteStatus status,
        String notes,
        BigDecimal laborCost,
        BigDecimal freightCost,
        BigDecimal totalAmount,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        List<QuoteItemResponse.Glass> glassItems,
        List<QuoteItemResponse.Kit> kitItems
) {
    public static QuoteResponse from(QuoteEntity entity, Map<UUID, String> glassNames, Map<UUID, String> kitNames) {
        List<QuoteItemResponse.Glass> glasses = entity.getGlassItems().stream()
                .map(item -> QuoteItemResponse.Glass.from(item, glassNames.getOrDefault(item.getGlassId(), "Vidro")))
                .toList();

        List<QuoteItemResponse.Kit> kits = entity.getKitItems().stream()
                .map(item -> QuoteItemResponse.Kit.from(item, kitNames.getOrDefault(item.getKitId(), "Kit")))
                .toList();

        return new QuoteResponse(
                entity.getId(),
                entity.getQuoteNumber(),
                entity.getCustomerName(),
                entity.getCustomerPhone(),
                entity.getCustomerEmail(),
                entity.getCustomerAddress(),
                entity.getStatus(),
                entity.getNotes(),
                entity.getLaborCost(),
                entity.getFreightCost(),
                entity.getTotalAmount(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                glasses,
                kits
        );
    }
}
