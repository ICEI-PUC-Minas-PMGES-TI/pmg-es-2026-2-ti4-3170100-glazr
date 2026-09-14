package com.glazr.glass.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateGlassRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 3, fraction = 2) BigDecimal thicknessMm,
        @NotBlank @Size(max = 80) String finish,
        @NotBlank @Size(max = 60) String color,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal pricePerSquareMeter
) {
}
