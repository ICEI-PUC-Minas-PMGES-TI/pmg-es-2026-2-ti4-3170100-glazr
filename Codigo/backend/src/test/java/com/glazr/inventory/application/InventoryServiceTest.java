package com.glazr.inventory.application;

import com.glazr.glass.infrastructure.GlassEntity;
import com.glazr.glass.infrastructure.GlassRepository;
import com.glazr.inventory.api.GlassStockSummaryResponse;
import com.glazr.inventory.domain.StockStatus;
import com.glazr.inventory.infrastructure.GlassSheetRepository;
import com.glazr.inventory.infrastructure.GlassStockSettingEntity;
import com.glazr.inventory.infrastructure.GlassStockSettingRepository;
import com.glazr.inventory.infrastructure.StockMovementRepository;
import com.glazr.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);

    @Mock private GlassRepository glasses;
    @Mock private GlassStockSettingRepository settings;
    @Mock private GlassSheetRepository sheets;
    @Mock private StockMovementRepository movements;

    private InventoryService service;
    private GlassEntity glass;

    @BeforeEach
    void setUp() {
        service = new InventoryService(glasses, settings, sheets, movements, CLOCK);
        glass = GlassEntity.create(
                "Vidro Temperado", new BigDecimal("8.00"), "Lapidado", "Incolor",
                new BigDecimal("180.00"), CLOCK.instant());
    }

    @Test
    void marksConfiguredStockAsLowWhenAvailableAreaReachesMinimum() {
        GlassStockSettingEntity setting = GlassStockSettingEntity.create(
                glass.getId(), new BigDecimal("4.000000"), CLOCK.instant());
        when(glasses.findById(glass.getId())).thenReturn(Optional.of(glass));
        when(sheets.sumRemainingAreaByGlassId(glass.getId())).thenReturn(new BigDecimal("4.000000"));
        when(settings.findById(glass.getId())).thenReturn(Optional.of(setting));

        GlassStockSummaryResponse result = service.getSummary(glass.getId());

        assertThat(result.availableAreaM2()).isEqualByComparingTo("4.000000");
        assertThat(result.minimumAreaM2()).isEqualByComparingTo("4.000000");
        assertThat(result.status()).isEqualTo(StockStatus.LOW);
    }

    @Test
    void keepsStockUnconfiguredUntilTheGlassHasAMinimum() {
        when(glasses.findById(glass.getId())).thenReturn(Optional.of(glass));
        when(sheets.sumRemainingAreaByGlassId(glass.getId())).thenReturn(null);
        when(settings.findById(glass.getId())).thenReturn(Optional.empty());

        GlassStockSummaryResponse result = service.getSummary(glass.getId());

        assertThat(result.availableAreaM2()).isEqualByComparingTo("0.000000");
        assertThat(result.minimumAreaM2()).isNull();
        assertThat(result.status()).isEqualTo(StockStatus.UNCONFIGURED);
    }

    @Test
    void returnsOnlyConfiguredLowStockInAlerts() {
        GlassEntity configured = glass;
        GlassEntity unconfigured = GlassEntity.create(
                "Vidro Laminado", new BigDecimal("10.00"), "Bisotê", "Fumê",
                new BigDecimal("250.00"), CLOCK.instant());
        GlassStockSettingEntity setting = GlassStockSettingEntity.create(
                configured.getId(), new BigDecimal("2.000000"), CLOCK.instant());
        when(glasses.findAll()).thenReturn(List.of(configured, unconfigured));
        when(sheets.sumRemainingAreaByGlassId(configured.getId())).thenReturn(new BigDecimal("1.500000"));
        when(sheets.sumRemainingAreaByGlassId(unconfigured.getId())).thenReturn(BigDecimal.ZERO);
        when(settings.findById(configured.getId())).thenReturn(Optional.of(setting));
        when(settings.findById(unconfigured.getId())).thenReturn(Optional.empty());

        List<GlassStockSummaryResponse> alerts = service.listAlerts();

        assertThat(alerts).extracting(GlassStockSummaryResponse::glassId).containsExactly(configured.getId());
    }

    @Test
    void updatesTheMinimumForAnExistingGlass() {
        AtomicReference<GlassStockSettingEntity> persisted = new AtomicReference<>();
        when(glasses.findById(glass.getId())).thenReturn(Optional.of(glass));
        when(settings.findById(glass.getId())).thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
        when(settings.saveAndFlush(any(GlassStockSettingEntity.class))).thenAnswer(invocation -> {
            GlassStockSettingEntity setting = invocation.getArgument(0);
            persisted.set(setting);
            return setting;
        });
        when(sheets.sumRemainingAreaByGlassId(glass.getId())).thenReturn(new BigDecimal("6.000000"));

        GlassStockSummaryResponse result = service.updateMinimum(glass.getId(), new BigDecimal("5.500000"));

        ArgumentCaptor<GlassStockSettingEntity> captor = ArgumentCaptor.forClass(GlassStockSettingEntity.class);
        verify(settings).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMinimumAreaM2()).isEqualByComparingTo("5.500000");
        assertThat(result.status()).isEqualTo(StockStatus.OK);
    }

    @Test
    void rejectsARequestForAnUnknownGlass() {
        when(glasses.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(java.util.UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Vidro não encontrado");
    }
}
