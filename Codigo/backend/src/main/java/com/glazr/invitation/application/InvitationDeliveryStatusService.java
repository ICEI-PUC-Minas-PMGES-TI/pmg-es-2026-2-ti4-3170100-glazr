package com.glazr.invitation.application;

import com.glazr.invitation.infrastructure.EmployeeInvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class InvitationDeliveryStatusService {
    private final EmployeeInvitationRepository invitations;
    private final Clock clock;

    public InvitationDeliveryStatusService(EmployeeInvitationRepository invitations, Clock clock) {
        this.invitations = invitations;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID id) {
        invitations.findById(id).ifPresent(invitation -> invitation.markSent(clock.instant()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id) {
        invitations.findById(id).ifPresent(invitation -> invitation.markDeliveryFailed("SMTP_DELIVERY_FAILED"));
    }
}
