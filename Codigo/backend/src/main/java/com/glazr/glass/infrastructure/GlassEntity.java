package com.glazr.glass.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "glasses")
public class GlassEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "thickness_mm", nullable = false, precision = 5, scale = 2)
    private BigDecimal thicknessMm;

    @Column(nullable = false, length = 80)
    private String finish;

    @Column(nullable = false, length = 60)
    private String color;

    @Column(name = "price_per_square_meter", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerSquareMeter;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected GlassEntity() {
    }

    public static GlassEntity create(String name, BigDecimal thicknessMm, String finish, String color,
                                     BigDecimal pricePerSquareMeter, Instant now) {
        GlassEntity glass = new GlassEntity();
        glass.id = UUID.randomUUID();
        glass.name = normalizeName(name);
        glass.thicknessMm = normalizeThickness(thicknessMm);
        glass.finish = normalizeFinish(finish);
        glass.color = normalizeColor(color);
        glass.pricePerSquareMeter = normalizePrice(pricePerSquareMeter);
        glass.active = true;
        glass.createdAt = now;
        glass.updatedAt = now;
        return glass;
    }

    public void update(String name, BigDecimal thicknessMm, String finish, String color,
                       BigDecimal pricePerSquareMeter, Instant now) {
        this.name = normalizeName(name);
        this.thicknessMm = normalizeThickness(thicknessMm);
        this.finish = normalizeFinish(finish);
        this.color = normalizeColor(color);
        this.pricePerSquareMeter = normalizePrice(pricePerSquareMeter);
        this.updatedAt = now;
    }

    public void updateStatus(boolean active, Instant now) {
        this.active = active;
        this.updatedAt = now;
    }

    private static String normalizeName(String value) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        String normalized = value.strip();
        if (normalized.length() > 120) throw new IllegalArgumentException("Nome deve ter no máximo 120 caracteres");
        return normalized;
    }

    private static BigDecimal normalizeThickness(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Espessura deve ser maior que zero");
        }
        return value;
    }

    private static String normalizeFinish(String value) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException("Acabamento é obrigatório");
        String normalized = value.strip();
        if (normalized.length() > 80) throw new IllegalArgumentException("Acabamento deve ter no máximo 80 caracteres");
        return normalized;
    }

    private static String normalizeColor(String value) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException("Cor é obrigatória");
        String normalized = value.strip();
        if (normalized.length() > 60) throw new IllegalArgumentException("Cor deve ter no máximo 60 caracteres");
        return normalized;
    }

    private static BigDecimal normalizePrice(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço por m² deve ser maior que zero");
        }
        return value;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getThicknessMm() { return thicknessMm; }
    public String getFinish() { return finish; }
    public String getColor() { return color; }
    public BigDecimal getPricePerSquareMeter() { return pricePerSquareMeter; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
