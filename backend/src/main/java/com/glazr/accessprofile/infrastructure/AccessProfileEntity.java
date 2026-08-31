package com.glazr.accessprofile.infrastructure;

import com.glazr.permission.infrastructure.PermissionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "access_profiles")
public class AccessProfileEntity {

    public static final UUID ADMINISTRATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(length = 500)
    private String description;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "system_profile", nullable = false)
    private boolean systemProfile;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "access_profile_permissions",
            joinColumns = @JoinColumn(name = "access_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_key"))
    private Set<PermissionEntity> permissions = new LinkedHashSet<>();

    protected AccessProfileEntity() {
    }

    public static AccessProfileEntity create(String name, String description, Set<PermissionEntity> permissions, Instant now) {
        AccessProfileEntity profile = new AccessProfileEntity();
        profile.id = UUID.randomUUID();
        profile.name = normalizeName(name);
        profile.description = normalizeDescription(description);
        profile.active = true;
        profile.systemProfile = false;
        profile.createdAt = now;
        profile.updatedAt = now;
        profile.permissions.addAll(permissions);
        return profile;
    }

    public void replace(String name, String description, boolean active, Set<PermissionEntity> permissions, Instant now) {
        if (systemProfile) throw new IllegalStateException("O perfil de sistema é protegido");
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.active = active;
        this.permissions.clear();
        this.permissions.addAll(permissions);
        this.updatedAt = now;
    }

    private static String normalizeName(String value) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        String normalized = value.strip();
        if (normalized.length() > 80) throw new IllegalArgumentException("Nome deve ter no máximo 80 caracteres");
        return normalized;
    }

    private static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip();
        if (normalized.length() > 500) throw new IllegalArgumentException("Descrição deve ter no máximo 500 caracteres");
        return normalized;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public boolean isSystemProfile() { return systemProfile; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<PermissionEntity> getPermissions() { return Set.copyOf(permissions); }
}
