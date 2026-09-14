package com.glazr.glass.api;

import com.glazr.glass.infrastructure.GlassEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GlassResponse(
        UUID id,
        String name,
        BigDecimal thicknessMm,
        String finish,
        String color,
        BigDecimal pricePerSquareMeter,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static GlassResponse from(GlassEntity glass) {
        return new GlassResponse(
                glass.getId(),
                glass.getName(),
                glass.getThicknessMm(),
                glass.getFinish(),
                glass.getColor(),
                glass.getPricePerSquareMeter(),
                glass.isActive(),
                glass.getCreatedAt(),
                glass.getUpdatedAt()
        );
    }
}
