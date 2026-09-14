package com.glazr.kit.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "kit_components")
public class KitComponentEntity {

    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false)
    private int quantity;
    @Column(length = 300)
    private String notes;

    protected KitComponentEntity() {
    }

    public static KitComponentEntity create(String name, int quantity, String notes) {
        KitComponentEntity component = new KitComponentEntity();
        component.id = UUID.randomUUID();
        component.name = normalizeName(name);
        component.quantity = normalizeQuantity(quantity);
        component.notes = normalizeNotes(notes);
        return component;
    }

    private static String normalizeName(String value) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException("Nome do componente é obrigatório");
        String normalized = value.strip();
        if (normalized.length() > 120) throw new IllegalArgumentException("Nome do componente deve ter no máximo 120 caracteres");
        return normalized;
    }

    private static int normalizeQuantity(int value) {
        if (value < 1) throw new IllegalArgumentException("Quantidade do componente deve ser maior que zero");
        return value;
    }

    private static String normalizeNotes(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip();
        if (normalized.length() > 300) throw new IllegalArgumentException("Observação deve ter no máximo 300 caracteres");
        return normalized;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public String getNotes() { return notes; }
}