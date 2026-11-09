package com.glazr.quote.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "quote_glass_items")
public class QuoteGlassItemEntity {

    private static final BigDecimal SQUARE_MM_PER_M2 = new BigDecimal("1000000");

    @Id
    private UUID id;

    @Column(name = "glass_id", nullable = false)
    private UUID glassId;

    @Column(name = "width_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal widthMm;

    @Column(name = "height_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal heightMm;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_area_m2", nullable = false, precision = 14, scale = 6)
    private BigDecimal unitAreaM2;

    @Column(name = "total_area_m2", nullable = false, precision = 14, scale = 6)
    private BigDecimal totalAreaM2;

    @Column(name = "price_per_square_meter", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerSquareMeter;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 300)
    private String notes;

    protected QuoteGlassItemEntity() {
    }

    public static QuoteGlassItemEntity create(UUID glassId, BigDecimal widthMm, BigDecimal heightMm,
                                              int quantity, BigDecimal pricePerSquareMeter, String notes) {
        if (glassId == null) throw new IllegalArgumentException("Vidro é obrigatório");
        if (widthMm == null || widthMm.signum() <= 0) throw new IllegalArgumentException("Largura deve ser positiva");
        if (heightMm == null || heightMm.signum() <= 0) throw new IllegalArgumentException("Altura deve ser positiva");
        if (quantity <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");
        if (pricePerSquareMeter == null || pricePerSquareMeter.signum() < 0) {
            throw new IllegalArgumentException("Preço por m² não pode ser negativo");
        }

        QuoteGlassItemEntity item = new QuoteGlassItemEntity();
        item.id = UUID.randomUUID();
        item.glassId = glassId;
        item.widthMm = widthMm.setScale(2, RoundingMode.HALF_UP);
        item.heightMm = heightMm.setScale(2, RoundingMode.HALF_UP);
        item.quantity = quantity;
        item.unitAreaM2 = widthMm.multiply(heightMm).divide(SQUARE_MM_PER_M2, 6, RoundingMode.HALF_UP);
        item.totalAreaM2 = item.unitAreaM2.multiply(BigDecimal.valueOf(quantity)).setScale(6, RoundingMode.HALF_UP);
        item.pricePerSquareMeter = pricePerSquareMeter.setScale(2, RoundingMode.HALF_UP);
        item.totalPrice = item.totalAreaM2.multiply(item.pricePerSquareMeter).setScale(2, RoundingMode.HALF_UP);
        item.notes = (notes != null && !notes.isBlank()) ? notes.strip() : null;
        return item;
    }

    public UUID getId() { return id; }
    public UUID getGlassId() { return glassId; }
    public BigDecimal getWidthMm() { return widthMm; }
    public BigDecimal getHeightMm() { return heightMm; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitAreaM2() { return unitAreaM2; }
    public BigDecimal getTotalAreaM2() { return totalAreaM2; }
    public BigDecimal getPricePerSquareMeter() { return pricePerSquareMeter; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getNotes() { return notes; }
}
