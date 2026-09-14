package com.glazr.inventory.infrastructure;

import com.glazr.inventory.domain.StockMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
public class StockMovementEntity {

    @Id
    private UUID id;

    @Column(name = "glass_sheet_id", nullable = false)
    private UUID glassSheetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;

    @Column(name = "previous_remaining_area_m2", precision = 14, scale = 6)
    private BigDecimal previousRemainingAreaM2;

    @Column(name = "resulting_remaining_area_m2", nullable = false, precision = 14, scale = 6)
    private BigDecimal resultingRemainingAreaM2;

    @Column(name = "piece_width_mm", precision = 10, scale = 2)
    private BigDecimal pieceWidthMm;

    @Column(name = "piece_height_mm", precision = 10, scale = 2)
    private BigDecimal pieceHeightMm;

    @Column(name = "piece_area_m2", precision = 14, scale = 6)
    private BigDecimal pieceAreaM2;

    @Column(name = "loss_area_m2", precision = 14, scale = 6)
    private BigDecimal lossAreaM2;

    @Column(length = 300)
    private String notes;

    @Column(name = "performed_by_user_id", nullable = false)
    private UUID performedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockMovementEntity() {
    }

    public static StockMovementEntity receipt(GlassSheetEntity sheet, String notes, UUID performedByUserId, Instant now) {
        return create(
                sheet.getId(), StockMovementType.RECEIPT, null, sheet.getRemainingAreaM2(),
                null, null, null, null, notes, performedByUserId, now
        );
    }

    public static StockMovementEntity adjustment(GlassSheetEntity sheet, BigDecimal previousRemainingAreaM2,
                                                 String notes, UUID performedByUserId, Instant now) {
        return create(
                sheet.getId(), StockMovementType.ADJUSTMENT, previousRemainingAreaM2, sheet.getRemainingAreaM2(),
                null, null, null, null, notes, performedByUserId, now
        );
    }

    public static StockMovementEntity manualCut(GlassSheetEntity sheet, BigDecimal previousRemainingAreaM2,
                                                BigDecimal pieceWidthMm, BigDecimal pieceHeightMm, BigDecimal pieceAreaM2,
                                                BigDecimal lossAreaM2, String notes, UUID performedByUserId, Instant now) {
        return create(
                sheet.getId(), StockMovementType.MANUAL_CUT, previousRemainingAreaM2, sheet.getRemainingAreaM2(),
                pieceWidthMm, pieceHeightMm, pieceAreaM2, lossAreaM2, notes, performedByUserId, now
        );
    }

    private static StockMovementEntity create(UUID glassSheetId, StockMovementType movementType,
                                              BigDecimal previousRemainingAreaM2, BigDecimal resultingRemainingAreaM2,
                                              BigDecimal pieceWidthMm, BigDecimal pieceHeightMm, BigDecimal pieceAreaM2,
                                              BigDecimal lossAreaM2, String notes, UUID performedByUserId, Instant now) {
        StockMovementEntity movement = new StockMovementEntity();
        movement.id = UUID.randomUUID();
        movement.glassSheetId = glassSheetId;
        movement.movementType = movementType;
        movement.previousRemainingAreaM2 = previousRemainingAreaM2;
        movement.resultingRemainingAreaM2 = resultingRemainingAreaM2;
        movement.pieceWidthMm = pieceWidthMm;
        movement.pieceHeightMm = pieceHeightMm;
        movement.pieceAreaM2 = pieceAreaM2;
        movement.lossAreaM2 = lossAreaM2;
        movement.notes = normalizeNotes(notes);
        movement.performedByUserId = performedByUserId;
        movement.createdAt = now;
        return movement;
    }

    private static String normalizeNotes(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip();
        if (normalized.length() > 300) throw new IllegalArgumentException("Observação deve ter no máximo 300 caracteres");
        return normalized;
    }

    public UUID getId() { return id; }
    public UUID getGlassSheetId() { return glassSheetId; }
    public StockMovementType getMovementType() { return movementType; }
    public BigDecimal getPreviousRemainingAreaM2() { return previousRemainingAreaM2; }
    public BigDecimal getResultingRemainingAreaM2() { return resultingRemainingAreaM2; }
    public BigDecimal getPieceWidthMm() { return pieceWidthMm; }
    public BigDecimal getPieceHeightMm() { return pieceHeightMm; }
    public BigDecimal getPieceAreaM2() { return pieceAreaM2; }
    public BigDecimal getLossAreaM2() { return lossAreaM2; }
    public String getNotes() { return notes; }
    public UUID getPerformedByUserId() { return performedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
