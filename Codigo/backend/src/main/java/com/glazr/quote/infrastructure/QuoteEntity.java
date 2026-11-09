package com.glazr.quote.infrastructure;

import com.glazr.quote.domain.QuoteStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class QuoteEntity {

    @Id
    private UUID id;

    @Column(name = "quote_number", insertable = false, updatable = false)
    private Long quoteNumber;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "customer_email", length = 120)
    private String customerEmail;

    @Column(name = "customer_address", length = 250)
    private String customerAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuoteStatus status;

    @Column(length = 500)
    private String notes;

    @Column(name = "labor_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "freight_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal freightCost;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "quote_id", nullable = false)
    private List<QuoteGlassItemEntity> glassItems = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "quote_id", nullable = false)
    private List<QuoteKitItemEntity> kitItems = new ArrayList<>();

    protected QuoteEntity() {
    }

    public static QuoteEntity create(String customerName, String customerPhone, String customerEmail,
                                     String customerAddress, String notes, BigDecimal laborCost,
                                     BigDecimal freightCost, List<QuoteGlassItemEntity> glassItems,
                                     List<QuoteKitItemEntity> kitItems, UUID createdByUserId, Instant now) {
        QuoteEntity quote = new QuoteEntity();
        quote.id = UUID.randomUUID();
        quote.customerName = normalizeRequired(customerName, "Nome do cliente", 120);
        quote.customerPhone = normalizeOptional(customerPhone, 30);
        quote.customerEmail = normalizeOptional(customerEmail, 120);
        quote.customerAddress = normalizeOptional(customerAddress, 250);
        quote.notes = normalizeOptional(notes, 500);
        quote.laborCost = normalizeNonNegative(laborCost, "Mão de obra");
        quote.freightCost = normalizeNonNegative(freightCost, "Frete");
        quote.status = QuoteStatus.DRAFT;
        quote.createdByUserId = createdByUserId;
        quote.createdAt = now;
        quote.updatedAt = now;

        if (glassItems != null) quote.glassItems.addAll(glassItems);
        if (kitItems != null) quote.kitItems.addAll(kitItems);

        quote.recalculateTotal();
        return quote;
    }

    public void update(String customerName, String customerPhone, String customerEmail,
                       String customerAddress, String notes, BigDecimal laborCost,
                       BigDecimal freightCost, List<QuoteGlassItemEntity> glassItems,
                       List<QuoteKitItemEntity> kitItems, Instant now) {
        if (this.status == QuoteStatus.COMPLETED || this.status == QuoteStatus.CANCELLED) {
            throw new IllegalStateException("Não é possível alterar um pedido finalizado ou cancelado");
        }
        this.customerName = normalizeRequired(customerName, "Nome do cliente", 120);
        this.customerPhone = normalizeOptional(customerPhone, 30);
        this.customerEmail = normalizeOptional(customerEmail, 120);
        this.customerAddress = normalizeOptional(customerAddress, 250);
        this.notes = normalizeOptional(notes, 500);
        this.laborCost = normalizeNonNegative(laborCost, "Mão de obra");
        this.freightCost = normalizeNonNegative(freightCost, "Frete");

        this.glassItems.clear();
        if (glassItems != null) this.glassItems.addAll(glassItems);

        this.kitItems.clear();
        if (kitItems != null) this.kitItems.addAll(kitItems);

        this.recalculateTotal();
        this.updatedAt = now;
    }

    public void updateStatus(QuoteStatus newStatus, Instant now) {
        if (newStatus == null) throw new IllegalArgumentException("Novo status é obrigatório");
        this.status = newStatus;
        this.updatedAt = now;
    }

    public void recalculateTotal() {
        BigDecimal glassTotal = glassItems.stream()
                .map(QuoteGlassItemEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal kitTotal = kitItems.stream()
                .map(QuoteKitItemEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = glassTotal.add(kitTotal).add(this.laborCost).add(this.freightCost).setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeRequired(String value, String field, int maxLength) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException(field + " é obrigatório");
        String normalized = value.strip();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " deve ter no máximo " + maxLength + " caracteres");
        return normalized;
    }

    private static String normalizeOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip();
        if (normalized.length() > maxLength) throw new IllegalArgumentException("Campo deve ter no máximo " + maxLength + " caracteres");
        return normalized;
    }

    private static BigDecimal normalizeNonNegative(BigDecimal value, String field) {
        if (value == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (value.signum() < 0) throw new IllegalArgumentException(field + " não pode ser negativo");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public Long getQuoteNumber() { return quoteNumber; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerAddress() { return customerAddress; }
    public QuoteStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public BigDecimal getLaborCost() { return laborCost; }
    public BigDecimal getFreightCost() { return freightCost; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<QuoteGlassItemEntity> getGlassItems() { return List.copyOf(glassItems); }
    public List<QuoteKitItemEntity> getKitItems() { return List.copyOf(kitItems); }
}
