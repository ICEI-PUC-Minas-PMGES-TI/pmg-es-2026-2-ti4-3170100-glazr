package com.glazr.password.application;

import com.glazr.mail.application.EmployeeMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetDeliveryListener {
    private final EmployeeMailSender mailSender;

    public PasswordResetDeliveryListener(EmployeeMailSender mailSender) { this.mailSender = mailSender; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(PasswordResetDeliveryRequested event) {
        try {
            mailSender.sendPasswordReset(event.message());
        } catch (RuntimeException ignored) {
            // A resposta permanece genérica; uma nova solicitação poderá ser feita.
        }
    }
}
