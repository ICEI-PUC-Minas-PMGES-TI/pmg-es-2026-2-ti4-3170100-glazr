package com.glazr.kit.infrastructure;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kits")
public class KitEntity {

    @Id
    private UUID id;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(nullable = false, length = 32)
    private String color;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "kit_id", nullable = false)
    private List<KitComponentEntity> components = new ArrayList<>();

    protected KitEntity() {
    }

    public static KitEntity create(String name, String color, BigDecimal price,
                                   List<KitComponentEntity> components, Instant now) {
        KitEntity kit = new KitEntity();
        kit.id = UUID.randomUUID();
        kit.name = normalizeName(name);
        kit.color = normalizeColor(color);
        kit.price = normalizePrice(price);
        kit.createdAt = now;
        kit.updatedAt = now;
        kit.replaceComponents(components);
        return kit;
    }

    public void replace(String name, String color, BigDecimal price,
                        List<KitComponentEntity> components, Instant now) {
        this.name = normalizeName(name);
        this.color = normalizeColor(color);
        this.price = normalizePrice(price);
        this.replaceComponents(components);
        this.updatedAt = now;
    }

    private void replaceComponents(List<KitComponentEntity> components) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("Ao menos um componente é obrigatório");
        }
        this.components.clear();
        this.components.addAll(components);
    }

    private static String normalizeName(String value) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        String normalized = value.strip();
        if (normalized.length() > 80) throw new IllegalArgumentException("Nome deve ter no máximo 80 caracteres");
        return normalized;
    }

    private static String normalizeColor(String value) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException("Cor é obrigatória");
        String normalized = value.strip();
        if (normalized.length() > 32) throw new IllegalArgumentException("Cor deve ter no máximo 32 caracteres");
        return normalized;
    }

    private static BigDecimal normalizePrice(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("Preço não pode ser negativo");
        return value;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public BigDecimal getPrice() { return price; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<KitComponentEntity> getComponents() { return List.copyOf(components); }
}