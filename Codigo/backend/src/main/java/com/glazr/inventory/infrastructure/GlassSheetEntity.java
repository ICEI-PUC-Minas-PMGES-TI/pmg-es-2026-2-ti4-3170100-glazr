package com.glazr.inventory.infrastructure;

import com.glazr.inventory.domain.SheetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "glass_sheets")
public class GlassSheetEntity {

    @Id
    private UUID id;

    @Column(name = "glass_id", nullable = false)
    private UUID glassId;

    @Column(name = "original_width_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalWidthMm;

    @Column(name = "original_height_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalHeightMm;

    @Column(name = "original_area_m2", nullable = false, precision = 14, scale = 6)
    private BigDecimal originalAreaM2;

    @Column(name = "remaining_area_m2", nullable = false, precision = 14, scale = 6)
    private BigDecimal remainingAreaM2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SheetStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected GlassSheetEntity() {
    }

    public UUID getId() { return id; }
    public UUID getGlassId() { return glassId; }
    public BigDecimal getOriginalWidthMm() { return originalWidthMm; }
    public BigDecimal getOriginalHeightMm() { return originalHeightMm; }
    public BigDecimal getOriginalAreaM2() { return originalAreaM2; }
    public BigDecimal getRemainingAreaM2() { return remainingAreaM2; }
    public SheetStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
