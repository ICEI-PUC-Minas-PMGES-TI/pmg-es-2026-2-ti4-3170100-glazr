package com.glazr.quote.api;

import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import com.glazr.quote.application.QuoteService;
import com.glazr.quote.domain.QuoteStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {

    private final QuoteService quotes;

    public QuoteController(QuoteService quotes) {
        this.quotes = quotes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse create(@Valid @RequestBody CreateQuoteRequest request,
                                @AuthenticationPrincipal GlazrUserPrincipal principal) {
        return quotes.create(request, principal.user().id());
    }

    @GetMapping
    public Page<QuoteResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) QuoteStatus status,
            Pageable pageable
    ) {
        return quotes.list(query, status, pageable);
    }

    @GetMapping("/{id}")
    public QuoteResponse get(@PathVariable UUID id) {
        return quotes.get(id);
    }

    @PutMapping("/{id}")
    public QuoteResponse update(@PathVariable UUID id, @Valid @RequestBody CreateQuoteRequest request) {
        return quotes.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public QuoteResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateQuoteStatusRequest request) {
        return quotes.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        quotes.delete(id);
    }
}
