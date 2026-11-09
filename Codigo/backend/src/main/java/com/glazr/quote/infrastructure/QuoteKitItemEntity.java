package com.glazr.quote.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "quote_kit_items")
public class QuoteKitItemEntity {

    @Id
    private UUID id;

    @Column(name = "kit_id", nullable = false)
    private UUID kitId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 300)
    private String notes;

    protected QuoteKitItemEntity() {
    }

    public static QuoteKitItemEntity create(UUID kitId, int quantity, BigDecimal unitPrice, String notes) {
        if (kitId == null) throw new IllegalArgumentException("Kit é obrigatório");
        if (quantity <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");
        if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("Preço unitário não pode ser negativo");

        QuoteKitItemEntity item = new QuoteKitItemEntity();
        item.id = UUID.randomUUID();
        item.kitId = kitId;
        item.quantity = quantity;
        item.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        item.totalPrice = item.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        item.notes = (notes != null && !notes.isBlank()) ? notes.strip() : null;
        return item;
    }

    public UUID getId() { return id; }
    public UUID getKitId() { return kitId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getNotes() { return notes; }
}
