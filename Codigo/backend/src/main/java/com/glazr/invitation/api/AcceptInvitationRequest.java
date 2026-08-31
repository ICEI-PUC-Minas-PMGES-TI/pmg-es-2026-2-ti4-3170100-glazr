package com.glazr.invitation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(@NotBlank @Size(max = 512) String token,
                                      @NotBlank @Size(min = 12, max = 128) String password) {
}
