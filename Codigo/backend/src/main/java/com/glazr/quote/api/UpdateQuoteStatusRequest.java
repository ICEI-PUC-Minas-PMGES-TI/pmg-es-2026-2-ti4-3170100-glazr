package com.glazr.quote.api;

import com.glazr.quote.domain.QuoteStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateQuoteStatusRequest(
        @NotNull(message = "Status é obrigatório")
        QuoteStatus status
) {}
