package com.glazr.quote.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateQuoteRequest(
        @NotBlank(message = "Nome do cliente é obrigatório")
        @Size(min = 2, max = 120, message = "Nome do cliente deve ter entre 2 e 120 caracteres")
        String customerName,

        @Pattern(regexp = "^$|^[0-9()+\\-\\s.]{8,30}$", message = "Telefone deve conter entre 8 e 30 dígitos/caracteres válidos (ex: (31) 98888-7777)")
        @Size(max = 30, message = "Telefone deve ter no máximo 30 caracteres")
        String customerPhone,

        @Pattern(regexp = "^$|^[\\w.%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "E-mail informado é inválido")
        @Size(max = 120, message = "E-mail deve ter no máximo 120 caracteres")
        String customerEmail,

        @Size(max = 250, message = "Endereço deve ter no máximo 250 caracteres")
        String customerAddress,

        @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
        String notes,

        @DecimalMin(value = "0.00", message = "Custo de mão de obra não pode ser negativo")
        BigDecimal laborCost,

        @DecimalMin(value = "0.00", message = "Custo de frete não pode ser negativo")
        BigDecimal freightCost,

        @Valid
        List<QuoteItemRequest.Glass> glassItems,

        @Valid
        List<QuoteItemRequest.Kit> kitItems
) {}

