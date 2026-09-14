package com.glazr.inventory.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "glass_stock_settings")
public class GlassStockSettingEntity {

    @Id
    @Column(name = "glass_id")
    private UUID glassId;

    @Column(name = "minimum_area_m2", precision = 14, scale = 6)
    private BigDecimal minimumAreaM2;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected GlassStockSettingEntity() {
    }

    public static GlassStockSettingEntity create(UUID glassId, BigDecimal minimumAreaM2, Instant now) {
        GlassStockSettingEntity setting = new GlassStockSettingEntity();
        setting.glassId = glassId;
        setting.minimumAreaM2 = normalizeMinimum(minimumAreaM2);
        setting.createdAt = now;
        setting.updatedAt = now;
        return setting;
    }

    public void updateMinimum(BigDecimal minimumAreaM2, Instant now) {
        this.minimumAreaM2 = normalizeMinimum(minimumAreaM2);
        this.updatedAt = now;
    }

    private static BigDecimal normalizeMinimum(BigDecimal value) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException("Estoque mínimo não pode ser negativo");
        }
        return value;
    }

    public UUID getGlassId() { return glassId; }
    public BigDecimal getMinimumAreaM2() { return minimumAreaM2; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
