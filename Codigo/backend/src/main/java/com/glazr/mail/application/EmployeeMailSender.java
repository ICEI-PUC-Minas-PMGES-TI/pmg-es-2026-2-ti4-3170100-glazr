package com.glazr.mail.application;

public interface EmployeeMailSender {
    void sendInvitation(EmployeeInvitationMessage message);
    void sendPasswordReset(PasswordResetMessage message);
}
