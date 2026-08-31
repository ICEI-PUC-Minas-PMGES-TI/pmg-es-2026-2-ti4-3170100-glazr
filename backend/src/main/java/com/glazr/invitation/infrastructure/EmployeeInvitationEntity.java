package com.glazr.invitation.infrastructure;

import com.glazr.invitation.domain.DeliveryStatus;
import com.glazr.invitation.domain.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee_invitations")
public class EmployeeInvitationEntity {

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;
    @Column(name = "delivery_error_code", length = 80) private String deliveryErrorCode;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "created_by_user_id", nullable = false) private UUID createdByUserId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected EmployeeInvitationEntity() {
    }

    public static EmployeeInvitationEntity create(UUID userId, UUID createdByUserId, String tokenHash,
                                                  Instant expiresAt, Instant createdAt) {
        if (userId == null || createdByUserId == null) throw new IllegalArgumentException("Usuários são obrigatórios");
        if (tokenHash == null || !tokenHash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Hash de token inválido");
        if (createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Validade do convite é inválida");
        }
        EmployeeInvitationEntity invitation = new EmployeeInvitationEntity();
        invitation.id = UUID.randomUUID();
        invitation.userId = userId;
        invitation.createdByUserId = createdByUserId;
        invitation.tokenHash = tokenHash;
        invitation.expiresAt = expiresAt;
        invitation.createdAt = createdAt;
        invitation.deliveryStatus = DeliveryStatus.PENDING;
        return invitation;
    }

    public InvitationStatus statusAt(Instant now) {
        if (consumedAt != null) return InvitationStatus.CONSUMED;
        if (revokedAt != null) return InvitationStatus.REVOKED;
        if (!expiresAt.isAfter(now)) return InvitationStatus.EXPIRED;
        return InvitationStatus.PENDING;
    }

    public void consume(Instant now) {
        if (statusAt(now) != InvitationStatus.PENDING) throw new IllegalStateException("Convite não pode ser consumido");
        consumedAt = now;
    }

    public void revoke(Instant now) {
        if (consumedAt != null) throw new IllegalStateException("Convite consumido não pode ser revogado");
        if (revokedAt == null) revokedAt = now;
    }

    public void markSent(Instant now) {
        deliveryStatus = DeliveryStatus.SENT;
        deliveryErrorCode = null;
        sentAt = now;
    }

    public void markDeliveryFailed(String safeCode) {
        if (safeCode == null || !safeCode.matches("[A-Z0-9_]{1,80}")) {
            throw new IllegalArgumentException("Código de entrega inválido");
        }
        deliveryStatus = DeliveryStatus.FAILED;
        deliveryErrorCode = safeCode;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public String getDeliveryErrorCode() { return deliveryErrorCode; }
    public Instant getSentAt() { return sentAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
