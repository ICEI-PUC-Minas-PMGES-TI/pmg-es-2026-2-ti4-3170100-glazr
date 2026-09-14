package com.glazr.inventory.api;

import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record UpdateStockMinimumRequest(
        @Digits(integer = 8, fraction = 6) BigDecimal minimumAreaM2
) {
}
