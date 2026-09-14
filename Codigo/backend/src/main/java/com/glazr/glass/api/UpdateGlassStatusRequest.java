package com.glazr.glass.api;

import jakarta.validation.constraints.NotNull;

public record UpdateGlassStatusRequest(
        @NotNull Boolean active
) {
}
