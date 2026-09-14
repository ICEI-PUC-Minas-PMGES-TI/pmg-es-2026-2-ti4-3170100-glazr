package com.glazr.kit.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KitComponentRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 300) String notes
) {
}