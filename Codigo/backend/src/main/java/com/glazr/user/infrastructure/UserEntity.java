package com.glazr.user.infrastructure;

import com.glazr.accessprofile.infrastructure.AccessProfileEntity;
import com.glazr.user.domain.AccountType;
import com.glazr.user.domain.EmailAddress;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;
    @Column(nullable = false, length = 320)
    private String email;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_access_profiles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "access_profile_id"))
    private Set<AccessProfileEntity> accessProfiles = new LinkedHashSet<>();

    protected UserEntity() {
    }

    public static UserEntity createEmployee(String email, String displayName,
                                            Set<AccessProfileEntity> profiles, Instant now) {
        String normalizedDisplayName = normalizeDisplayName(displayName);
        if (profiles == null || profiles.isEmpty()) throw new IllegalArgumentException("Ao menos um perfil é obrigatório");
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID();
        user.email = EmailAddress.normalize(email);
        user.displayName = normalizedDisplayName;
        user.accountType = AccountType.EMPLOYEE;
        user.active = true;
        user.createdAt = now;
        user.updatedAt = now;
        user.accessProfiles.addAll(profiles);
        return user;
    }

    public boolean isLocked(Instant now) { return lockedUntil != null && lockedUntil.isAfter(now); }

    public void recordFailedLogin(Instant now) {
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            failedLoginAttempts = 0;
            lockedUntil = null;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= 5) lockedUntil = now.plus(Duration.ofMinutes(15));
        updatedAt = now;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
        updatedAt = now;
    }

    public void update(String email, String displayName, Set<AccessProfileEntity> profiles, Instant now) {
        if (email != null) this.email = EmailAddress.normalize(email);
        if (displayName != null) {
            this.displayName = normalizeDisplayName(displayName);
        }
        if (profiles != null) {
            if (profiles.isEmpty()) throw new IllegalArgumentException("Ao menos um perfil é obrigatório");
            this.accessProfiles.clear();
            this.accessProfiles.addAll(profiles);
        }
        updatedAt = now;
    }

    private static String normalizeDisplayName(String value) {
        if (value == null || value.strip().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        String normalized = value.strip();
        if (normalized.length() > 120) throw new IllegalArgumentException("Nome deve ter no máximo 120 caracteres");
        return normalized;
    }

    public void updateStatus(boolean active, Instant now) {
        this.active = active;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public AccountType getAccountType() { return accountType; }
    public boolean isActive() { return active; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<AccessProfileEntity> getAccessProfiles() { return Set.copyOf(accessProfiles); }
}
