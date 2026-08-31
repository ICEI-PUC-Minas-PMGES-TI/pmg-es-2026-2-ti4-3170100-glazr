package com.glazr.mail.application;

import java.time.Instant;

public record EmployeeInvitationMessage(String recipientEmail, String displayName,
                                        String rawToken, Instant expiresAt) {
}
