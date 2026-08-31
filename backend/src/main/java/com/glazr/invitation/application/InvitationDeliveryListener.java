package com.glazr.invitation.application;

import com.glazr.mail.application.EmployeeMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvitationDeliveryListener {
    private final EmployeeMailSender mailSender;
    private final InvitationDeliveryStatusService statuses;

    public InvitationDeliveryListener(EmployeeMailSender mailSender, InvitationDeliveryStatusService statuses) {
        this.mailSender = mailSender;
        this.statuses = statuses;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(InvitationDeliveryRequested event) {
        try {
            mailSender.sendInvitation(event.message());
            statuses.markSent(event.invitationId());
        } catch (RuntimeException exception) {
            statuses.markFailed(event.invitationId());
        }
    }
}
