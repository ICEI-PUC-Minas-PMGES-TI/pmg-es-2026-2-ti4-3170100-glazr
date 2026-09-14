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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "glass_sheets")
public class GlassSheetEntity {

    private static final int AREA_SCALE = 6;
    private static final BigDecimal SQUARE_MILLIMETERS_PER_SQUARE_METER = new BigDecimal("1000000");

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

    public static GlassSheetEntity create(UUID glassId, BigDecimal originalWidthMm, BigDecimal originalHeightMm, Instant now) {
        GlassSheetEntity sheet = new GlassSheetEntity();
        sheet.id = UUID.randomUUID();
        sheet.glassId = glassId;
        sheet.originalWidthMm = normalizeDimension(originalWidthMm, "Largura da chapa");
        sheet.originalHeightMm = normalizeDimension(originalHeightMm, "Altura da chapa");
        sheet.originalAreaM2 = calculateArea(sheet.originalWidthMm, sheet.originalHeightMm);
        sheet.remainingAreaM2 = sheet.originalAreaM2;
        sheet.status = SheetStatus.AVAILABLE;
        sheet.createdAt = now;
        sheet.updatedAt = now;
        return sheet;
    }

    public void updateRemainingArea(BigDecimal remainingAreaM2, Instant now) {
        BigDecimal normalized = normalizeArea(remainingAreaM2, "Saldo disponível");
        if (normalized.compareTo(originalAreaM2) > 0) {
            throw new IllegalArgumentException("Saldo disponível não pode superar a área original da chapa");
        }
        this.remainingAreaM2 = normalized;
        this.status = normalized.signum() == 0 ? SheetStatus.DEPLETED : SheetStatus.AVAILABLE;
        this.updatedAt = now;
    }

    public static BigDecimal calculateArea(BigDecimal widthMm, BigDecimal heightMm) {
        return normalizeDimension(widthMm, "Largura").multiply(normalizeDimension(heightMm, "Altura"))
                .divide(SQUARE_MILLIMETERS_PER_SQUARE_METER, AREA_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeDimension(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " deve ser maior que zero");
        }
        return value;
    }

    private static BigDecimal normalizeArea(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " não pode ser negativo");
        }
        return value.setScale(AREA_SCALE, RoundingMode.HALF_UP);
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
