package com.glazr.password.application;

import com.glazr.mail.application.EmployeeMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetDeliveryListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetDeliveryListener.class);
    private final EmployeeMailSender mailSender;

    public PasswordResetDeliveryListener(EmployeeMailSender mailSender) { this.mailSender = mailSender; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailTaskExecutor")
    public void deliver(PasswordResetDeliveryRequested event) {
        try {
            mailSender.sendPasswordReset(event.message());
        } catch (RuntimeException ignored) {
            LOGGER.warn("PASSWORD_RESET_DELIVERY_FAILED");
        }
    }
}
