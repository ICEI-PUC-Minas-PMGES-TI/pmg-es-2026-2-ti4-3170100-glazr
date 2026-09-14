package com.glazr.inventory.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateSheetStockRequest(
        @NotNull @DecimalMin("0.000000") @Digits(integer = 8, fraction = 6) BigDecimal remainingAreaM2,
        @NotBlank @Size(max = 300) String notes
) {
    public record Cut(
            @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal pieceWidthMm,
            @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal pieceHeightMm,
            @NotNull @DecimalMin("0.000000") @Digits(integer = 8, fraction = 6) BigDecimal usableRemainderAreaM2,
            @Size(max = 300) String notes
    ) {
    }
}
