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
import com.glazr.quote.infrastructure.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private QuoteRepository quotes;
    @Mock
    private GlassRepository glasses;
    @Mock
    private KitRepository kits;

    private QuoteService service;
    private GlassEntity glass;
    private KitEntity kit;

    @BeforeEach
    void setUp() {
        service = new QuoteService(quotes, glasses, kits, CLOCK);

        glass = GlassEntity.create(
                "Vidro Temperado 8mm", new BigDecimal("8.00"), "Lapidado", "Incolor",
                new BigDecimal("200.00"), CLOCK.instant());

        kit = KitEntity.create(
                "Kit Box Banheiro", "Branco", new BigDecimal("350.00"),
                List.of(com.glazr.kit.infrastructure.KitComponentEntity.create("Roldana", 2, null)),
                CLOCK.instant());
    }

    @Test
    void createsQuoteWithCalculatedTotals() {
        when(glasses.findById(glass.getId())).thenReturn(Optional.of(glass));
        when(kits.findById(kit.getId())).thenReturn(Optional.of(kit));
        when(quotes.saveAndFlush(any(QuoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var glassReq = new QuoteItemRequest.Glass(
                glass.getId(), new BigDecimal("1000.00"), new BigDecimal("2000.00"), 1, "Peça porta");
        var kitReq = new QuoteItemRequest.Kit(kit.getId(), 1, "Box frontal");

        var request = new CreateQuoteRequest(
                "João da Silva", "31988887777", "joao@email.com", "Rua das Flores, 123",
                "Orçamento residencial", new BigDecimal("150.00"), new BigDecimal("50.00"),
                List.of(glassReq), List.of(kitReq));

        UUID userId = UUID.randomUUID();
        QuoteResponse response = service.create(request, userId);

        assertThat(response.customerName()).isEqualTo("João da Silva");
        assertThat(response.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(response.totalAmount()).isEqualByComparingTo("950.00");
        assertThat(response.glassItems()).hasSize(1);
        assertThat(response.kitItems()).hasSize(1);
    }

    @Test
    void updatesQuoteStatus() {
        QuoteEntity quote = QuoteEntity.create(
                "Maria Souza", "31999990000", null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, UUID.randomUUID(), CLOCK.instant());

        when(quotes.findById(quote.getId())).thenReturn(Optional.of(quote));
        when(quotes.saveAndFlush(any(QuoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        QuoteResponse response = service.updateStatus(quote.getId(), new UpdateQuoteStatusRequest(QuoteStatus.APPROVED));

        assertThat(response.status()).isEqualTo(QuoteStatus.APPROVED);
        verify(quotes).saveAndFlush(quote);
    }

    @Test
    void listsQuotesWithSearch() {
        QuoteEntity quote = QuoteEntity.create(
                "Carlos Pereira", "31988881111", null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, UUID.randomUUID(), CLOCK.instant());

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(quotes.search("Carlos", QuoteStatus.DRAFT, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(quote)));

        var page = service.list("Carlos", QuoteStatus.DRAFT, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).customerName()).isEqualTo("Carlos Pereira");
    }

    @Test
    void deletesQuote() {
        QuoteEntity quote = QuoteEntity.create(
                "Carlos Pereira", "31988881111", null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, UUID.randomUUID(), CLOCK.instant());

        when(quotes.findById(quote.getId())).thenReturn(Optional.of(quote));

        service.delete(quote.getId());

        verify(quotes).delete(quote);
        verify(quotes).flush();
    }
}
