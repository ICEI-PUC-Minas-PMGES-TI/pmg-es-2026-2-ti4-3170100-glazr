package com.glazr.inventory.application;

import com.glazr.glass.infrastructure.GlassEntity;
import com.glazr.glass.infrastructure.GlassRepository;
import com.glazr.inventory.api.GlassSheetResponse;
import com.glazr.inventory.api.GlassStockSummaryResponse;
import com.glazr.inventory.api.StockMovementResponse;
import com.glazr.inventory.domain.StockStatus;
import com.glazr.inventory.infrastructure.GlassSheetRepository;
import com.glazr.inventory.infrastructure.GlassStockSettingEntity;
import com.glazr.inventory.infrastructure.GlassStockSettingRepository;
import com.glazr.inventory.infrastructure.StockMovementRepository;
import com.glazr.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6);

    private final GlassRepository glasses;
    private final GlassStockSettingRepository settings;
    private final GlassSheetRepository sheets;
    private final StockMovementRepository movements;
    private final Clock clock;

    public InventoryService(GlassRepository glasses, GlassStockSettingRepository settings,
                            GlassSheetRepository sheets, StockMovementRepository movements, Clock clock) {
        this.glasses = glasses;
        this.settings = settings;
        this.sheets = sheets;
        this.movements = movements;
        this.clock = clock;
    }

    @PreAuthorize("hasAuthority('GLASS_READ')")
    @Transactional(readOnly = true)
    public Page<GlassStockSummaryResponse> list(Pageable pageable) {
        return glasses.findAll(pageable).map(this::summaryFor);
    }

    @PreAuthorize("hasAuthority('GLASS_READ')")
    @Transactional(readOnly = true)
    public List<GlassStockSummaryResponse> listAlerts() {
        return glasses.findAll().stream()
                .map(this::summaryFor)
                .filter(summary -> summary.status() == StockStatus.LOW)
                .toList();
    }

    @PreAuthorize("hasAuthority('GLASS_READ')")
    @Transactional(readOnly = true)
    public GlassStockSummaryResponse getSummary(UUID glassId) {
        return summaryFor(requireGlass(glassId));
    }

    @PreAuthorize("hasAuthority('GLASS_READ')")
    @Transactional(readOnly = true)
    public Page<GlassSheetResponse> listSheets(UUID glassId, Pageable pageable) {
        requireGlass(glassId);
        return sheets.findByGlassId(glassId, pageable).map(GlassSheetResponse::from);
    }

    @PreAuthorize("hasAuthority('GLASS_READ')")
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> listMovements(UUID glassId, Pageable pageable) {
        requireGlass(glassId);
        return movements.findByGlassId(glassId, pageable).map(StockMovementResponse::from);
    }

    @PreAuthorize("hasAuthority('GLASS_UPDATE')")
    @Transactional
    public GlassStockSummaryResponse updateMinimum(UUID glassId, BigDecimal minimumAreaM2) {
        GlassEntity glass = requireGlass(glassId);
        GlassStockSettingEntity setting = settings.findById(glassId)
                .orElseGet(() -> GlassStockSettingEntity.create(glassId, minimumAreaM2, clock.instant()));
        setting.updateMinimum(minimumAreaM2, clock.instant());
        settings.saveAndFlush(setting);
        return summaryFor(glass);
    }

    private GlassStockSummaryResponse summaryFor(GlassEntity glass) {
        BigDecimal availableAreaM2 = normalizeArea(sheets.sumRemainingAreaByGlassId(glass.getId()));
        BigDecimal minimumAreaM2 = settings.findById(glass.getId())
                .map(GlassStockSettingEntity::getMinimumAreaM2)
                .orElse(null);
        return GlassStockSummaryResponse.from(glass, availableAreaM2, minimumAreaM2,
                statusFor(availableAreaM2, minimumAreaM2));
    }

    private StockStatus statusFor(BigDecimal availableAreaM2, BigDecimal minimumAreaM2) {
        if (minimumAreaM2 == null) return StockStatus.UNCONFIGURED;
        return availableAreaM2.compareTo(minimumAreaM2) <= 0 ? StockStatus.LOW : StockStatus.OK;
    }

    private BigDecimal normalizeArea(BigDecimal area) {
        return area == null ? ZERO : area;
    }

    private GlassEntity requireGlass(UUID id) {
        return glasses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vidro"));
    }
}
