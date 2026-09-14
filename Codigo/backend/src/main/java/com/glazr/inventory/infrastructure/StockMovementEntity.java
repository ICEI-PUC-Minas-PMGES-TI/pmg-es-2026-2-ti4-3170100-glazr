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
