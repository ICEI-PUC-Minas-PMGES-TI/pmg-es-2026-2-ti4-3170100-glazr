package com.glazr.invitation.api;

import com.glazr.invitation.infrastructure.EmployeeInvitationEntity;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(UUID id, String status, Instant expiresAt, String deliveryStatus,
                                 String deliveryErrorCode, Instant sentAt) {
    public static InvitationResponse from(EmployeeInvitationEntity invitation, Instant now) {
        return new InvitationResponse(invitation.getId(), invitation.statusAt(now).name(),
                invitation.getExpiresAt(), invitation.getDeliveryStatus().name(),
                invitation.getDeliveryErrorCode(), invitation.getSentAt());
    }
}
