package com.glazr.password.application;

import com.glazr.mail.application.PasswordResetMessage;

public record PasswordResetDeliveryRequested(PasswordResetMessage message) {
}
