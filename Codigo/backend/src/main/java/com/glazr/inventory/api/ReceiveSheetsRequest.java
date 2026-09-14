package com.glazr.inventory.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReceiveSheetsRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal widthMm,
        @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal heightMm,
        @NotNull @Min(1) @Max(1000) Integer quantity,
        @Size(max = 300) String notes
) {
}
