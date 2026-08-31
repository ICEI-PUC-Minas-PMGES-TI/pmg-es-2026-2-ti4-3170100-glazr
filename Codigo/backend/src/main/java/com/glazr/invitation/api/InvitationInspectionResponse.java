package com.glazr.invitation.api;

import java.time.Instant;
import java.util.List;

public record InvitationInspectionResponse(String displayName, String email, Instant expiresAt,
                                           List<String> availableMethods) {
}
