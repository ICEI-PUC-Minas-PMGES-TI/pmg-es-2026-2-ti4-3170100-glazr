package com.glazr.kit.api;

import com.glazr.kit.infrastructure.KitComponentEntity;
import com.glazr.kit.infrastructure.KitEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record KitResponse(
        UUID id,
        String name,
        String color,
        BigDecimal price,
        List<Component> components,
        Instant createdAt,
        Instant updatedAt
) {

    public record Component(String name, int quantity, String notes) {
    }

    public static KitResponse from(KitEntity kit) {
        return new KitResponse(kit.getId(), kit.getName(), kit.getColor(), kit.getPrice(),
                kit.getComponents().stream()
                        .map(KitResponse::component)
                        .sorted(Comparator.comparing(Component::name))
                        .toList(),
                kit.getCreatedAt(), kit.getUpdatedAt());
    }

    private static Component component(KitComponentEntity entity) {
        return new Component(entity.getName(), entity.getQuantity(), entity.getNotes());
    }
}