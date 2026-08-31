package com.glazr.password.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompleteRequest(@NotBlank @Size(max = 512) String token,
                                           @NotBlank @Size(min = 12, max = 128) String newPassword) {
}
