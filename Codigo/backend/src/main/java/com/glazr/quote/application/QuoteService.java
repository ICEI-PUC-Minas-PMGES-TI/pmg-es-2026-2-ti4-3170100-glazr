package com.glazr.quote.application;

import com.glazr.glass.infrastructure.GlassEntity;
import com.glazr.glass.infrastructure.GlassRepository;
import com.glazr.kit.infrastructure.KitEntity;
import com.glazr.kit.infrastructure.KitRepository;
import com.glazr.quote.api.CreateQuoteRequest;
import com.glazr.quote.api.QuoteItemRequest;
import com.glazr.quote.api.QuoteResponse;
import com.glazr.quote.api.UpdateQuoteStatusRequest;
import com.glazr.quote.domain.QuoteStatus;
import com.glazr.quote.infrastructure.QuoteEntity;
import com.glazr.quote.infrastructure.QuoteGlassItemEntity;
import com.glazr.quote.infrastructure.QuoteKitItemEntity;
import com.glazr.quote.infrastructure.QuoteRepository;
import com.glazr.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuoteService {

    private final QuoteRepository quotes;
    private final GlassRepository glasses;
    private final KitRepository kits;
    private final Clock clock;

    public QuoteService(QuoteRepository quotes, GlassRepository glasses, KitRepository kits, Clock clock) {
        this.quotes = quotes;
        this.glasses = glasses;
        this.kits = kits;
        this.clock = clock;
    }

    @PreAuthorize("hasAuthority('QUOTE_CREATE')")
    @Transactional
    public QuoteResponse create(CreateQuoteRequest request, UUID createdByUserId) {
        List<QuoteGlassItemEntity> glassItems = resolveGlassItems(request.glassItems());
        List<QuoteKitItemEntity> kitItems = resolveKitItems(request.kitItems());

        QuoteEntity quote = QuoteEntity.create(
                request.customerName(),
                request.customerPhone(),
                request.customerEmail(),
                request.customerAddress(),
                request.notes(),
                request.laborCost(),
                request.freightCost(),
                glassItems,
                kitItems,
                createdByUserId,
                clock.instant()
        );

        QuoteEntity saved = quotes.saveAndFlush(quote);
        return toResponse(saved);
    }

    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Transactional(readOnly = true)
    public Page<QuoteResponse> list(String query, QuoteStatus status, Pageable pageable) {
        String cleanQuery = (query != null) ? query.strip() : "";
        Page<QuoteEntity> result = quotes.search(cleanQuery, status, pageable);
        return result.map(this::toResponse);
    }

    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Transactional(readOnly = true)
    public QuoteResponse get(UUID id) {
        return toResponse(requireQuote(id));
    }

    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Transactional
    public QuoteResponse update(UUID id, CreateQuoteRequest request) {
        QuoteEntity quote = requireQuote(id);
        List<QuoteGlassItemEntity> glassItems = resolveGlassItems(request.glassItems());
        List<QuoteKitItemEntity> kitItems = resolveKitItems(request.kitItems());

        quote.update(
                request.customerName(),
                request.customerPhone(),
                request.customerEmail(),
                request.customerAddress(),
                request.notes(),
                request.laborCost(),
                request.freightCost(),
                glassItems,
                kitItems,
                clock.instant()
        );

        QuoteEntity saved = quotes.saveAndFlush(quote);
        return toResponse(saved);
    }

    @PreAuthorize("hasAuthority('QUOTE_STATUS_UPDATE')")
    @Transactional
    public QuoteResponse updateStatus(UUID id, UpdateQuoteStatusRequest request) {
        QuoteEntity quote = requireQuote(id);
        quote.updateStatus(request.status(), clock.instant());
        QuoteEntity saved = quotes.saveAndFlush(quote);
        return toResponse(saved);
    }

    @PreAuthorize("hasAuthority('QUOTE_DELETE')")
    @Transactional
    public void delete(UUID id) {
        QuoteEntity quote = requireQuote(id);
        quotes.delete(quote);
        quotes.flush();
    }

    private QuoteEntity requireQuote(UUID id) {
        return quotes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Orçamento"));
    }

    private List<QuoteGlassItemEntity> resolveGlassItems(List<QuoteItemRequest.Glass> requests) {
        if (requests == null || requests.isEmpty()) return new ArrayList<>();
        List<QuoteGlassItemEntity> items = new ArrayList<>();
        for (var req : requests) {
            GlassEntity glass = glasses.findById(req.glassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vidro"));
            items.add(QuoteGlassItemEntity.create(
                    glass.getId(),
                    req.widthMm(),
                    req.heightMm(),
                    req.quantity(),
                    glass.getPricePerSquareMeter(),
                    req.notes()
            ));
        }
        return items;
    }

    private List<QuoteKitItemEntity> resolveKitItems(List<QuoteItemRequest.Kit> requests) {
        if (requests == null || requests.isEmpty()) return new ArrayList<>();
        List<QuoteKitItemEntity> items = new ArrayList<>();
        for (var req : requests) {
            KitEntity kit = kits.findById(req.kitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kit"));
            items.add(QuoteKitItemEntity.create(
                    kit.getId(),
                    req.quantity(),
                    kit.getPrice(),
                    req.notes()
            ));
        }
        return items;
    }

    private QuoteResponse toResponse(QuoteEntity entity) {
        Map<UUID, String> glassNames = new HashMap<>();
        for (var item : entity.getGlassItems()) {
            glasses.findById(item.getGlassId()).ifPresent(g -> glassNames.put(g.getId(), g.getName()));
        }

        Map<UUID, String> kitNames = new HashMap<>();
        for (var item : entity.getKitItems()) {
            kits.findById(item.getKitId()).ifPresent(k -> kitNames.put(k.getId(), k.getName()));
        }

        return QuoteResponse.from(entity, glassNames, kitNames);
    }
}
