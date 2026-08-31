package com.glazr.invitation.application;

import com.glazr.mail.application.EmployeeInvitationMessage;

import java.util.UUID;

public record InvitationDeliveryRequested(UUID invitationId, EmployeeInvitationMessage message) {
}
