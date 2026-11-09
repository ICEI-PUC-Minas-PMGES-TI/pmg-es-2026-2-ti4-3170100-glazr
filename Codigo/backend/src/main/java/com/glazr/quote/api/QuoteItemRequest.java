package com.glazr.quote.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class QuoteItemRequest {

    public record Glass(
            @NotNull(message = "Vidro é obrigatório")
            UUID glassId,

            @NotNull(message = "Largura é obrigatória")
            @DecimalMin(value = "10.00", message = "Largura deve ter no mínimo 10 mm")
            BigDecimal widthMm,

            @NotNull(message = "Altura é obrigatória")
            @DecimalMin(value = "10.00", message = "Altura deve ter no mínimo 10 mm")
            BigDecimal heightMm,

            @Min(value = 1, message = "Quantidade mínima é 1")
            int quantity,

            @Size(max = 300, message = "Observação deve ter no máximo 300 caracteres")
            String notes
    ) {}

    public record Kit(
            @NotNull(message = "Kit é obrigatório")
            UUID kitId,

            @Min(value = 1, message = "Quantidade mínima é 1")
            int quantity,

            @Size(max = 300, message = "Observação deve ter no máximo 300 caracteres")
            String notes
    ) {}
}
