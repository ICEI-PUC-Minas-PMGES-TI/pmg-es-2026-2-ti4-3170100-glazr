package com.glazr.inventory.application;

import com.glazr.glass.infrastructure.GlassEntity;
import com.glazr.glass.infrastructure.GlassRepository;
import com.glazr.inventory.api.GlassSheetResponse;
import com.glazr.inventory.api.GlassStockSummaryResponse;
import com.glazr.inventory.api.ManualCutResponse;
import com.glazr.inventory.api.ReceiveSheetsRequest;
import com.glazr.inventory.api.StockMovementResponse;
import com.glazr.inventory.api.UpdateSheetStockRequest;
import com.glazr.inventory.domain.StockStatus;
import com.glazr.inventory.infrastructure.GlassSheetEntity;
import com.glazr.inventory.infrastructure.GlassSheetRepository;
import com.glazr.inventory.infrastructure.GlassStockSettingEntity;
import com.glazr.inventory.infrastructure.GlassStockSettingRepository;
import com.glazr.inventory.infrastructure.StockMovementEntity;
import com.glazr.inventory.infrastructure.StockMovementRepository;
import com.glazr.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

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

    @PreAuthorize("hasAuthority('GLASS_UPDATE')")
    @Transactional
    public List<GlassSheetResponse> receive(UUID glassId, ReceiveSheetsRequest request, UUID performedByUserId) {
        requireGlass(glassId);
        List<GlassSheetEntity> receivedSheets = java.util.stream.IntStream.range(0, request.quantity())
                .mapToObj(ignored -> GlassSheetEntity.create(
                        glassId, request.widthMm(), request.heightMm(), clock.instant()))
                .toList();
        List<GlassSheetEntity> savedSheets = sheets.saveAll(receivedSheets);
        movements.saveAll(savedSheets.stream()
                .map(sheet -> StockMovementEntity.receipt(sheet, request.notes(), performedByUserId, clock.instant()))
                .toList());
        return savedSheets.stream().map(GlassSheetResponse::from).toList();
    }

    @PreAuthorize("hasAuthority('GLASS_UPDATE')")
    @Transactional
    public GlassSheetResponse adjust(UUID sheetId, UpdateSheetStockRequest request, UUID performedByUserId) {
        GlassSheetEntity sheet = requireSheetForUpdate(sheetId);
        BigDecimal previousRemainingAreaM2 = sheet.getRemainingAreaM2();
        if (previousRemainingAreaM2.compareTo(request.remainingAreaM2()) == 0) {
            throw new IllegalArgumentException("O ajuste deve alterar o saldo disponível");
        }
        sheet.updateRemainingArea(request.remainingAreaM2(), clock.instant());
        movements.save(StockMovementEntity.adjustment(
                sheet, previousRemainingAreaM2, request.notes(), performedByUserId, clock.instant()));
        return GlassSheetResponse.from(sheet);
    }

    @PreAuthorize("hasAuthority('GLASS_UPDATE')")
    @Transactional
    public ManualCutResponse cut(UUID sheetId, UpdateSheetStockRequest.Cut request, UUID performedByUserId) {
        GlassSheetEntity sheet = requireSheetForUpdate(sheetId);
        BigDecimal previousRemainingAreaM2 = sheet.getRemainingAreaM2();
        BigDecimal pieceAreaM2 = GlassSheetEntity.calculateArea(request.pieceWidthMm(), request.pieceHeightMm());
        BigDecimal usableRemainderAreaM2 = request.usableRemainderAreaM2().setScale(6, RoundingMode.HALF_UP);
        BigDecimal consumedAreaM2 = pieceAreaM2.add(usableRemainderAreaM2);
        if (consumedAreaM2.compareTo(previousRemainingAreaM2) > 0) {
            throw new IllegalArgumentException("Peça e sobra útil não podem superar o saldo disponível da chapa");
        }
        BigDecimal lossAreaM2 = previousRemainingAreaM2.subtract(consumedAreaM2);
        sheet.updateRemainingArea(usableRemainderAreaM2, clock.instant());
        StockMovementEntity movement = StockMovementEntity.manualCut(
                sheet, previousRemainingAreaM2, request.pieceWidthMm(), request.pieceHeightMm(), pieceAreaM2,
                lossAreaM2, request.notes(), performedByUserId, clock.instant());
        movements.save(movement);
        return ManualCutResponse.from(sheet, movement, yieldPercent(pieceAreaM2, lossAreaM2));
    }

    private GlassEntity requireGlass(UUID id) {
        return glasses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vidro"));
    }

    private GlassSheetEntity requireSheetForUpdate(UUID sheetId) {
        return sheets.findByIdForUpdate(sheetId).orElseThrow(() -> new ResourceNotFoundException("Chapa"));
    }

    private GlassStockSummaryResponse summaryFor(GlassEntity glass) {
        BigDecimal availableAreaM2 = normalizeArea(sheets.sumRemainingAreaByGlassId(glass.getId()));
        BigDecimal minimumAreaM2 = settings.findById(glass.getId())
                .map(GlassStockSettingEntity::getMinimumAreaM2)
                .orElse(null);
        return GlassStockSummaryResponse.from(glass, availableAreaM2, minimumAreaM2,
                statusFor(availableAreaM2, minimumAreaM2));
    }

    private BigDecimal normalizeArea(BigDecimal area) {
        return area == null ? ZERO : area;
    }

    private StockStatus statusFor(BigDecimal availableAreaM2, BigDecimal minimumAreaM2) {
        if (minimumAreaM2 == null) return StockStatus.UNCONFIGURED;
        return availableAreaM2.compareTo(minimumAreaM2) <= 0 ? StockStatus.LOW : StockStatus.OK;
    }

    private BigDecimal yieldPercent(BigDecimal pieceAreaM2, BigDecimal lossAreaM2) {
        BigDecimal processedAreaM2 = pieceAreaM2.add(lossAreaM2);
        return processedAreaM2.signum() == 0 ? ZERO : pieceAreaM2
                .multiply(ONE_HUNDRED)
                .divide(processedAreaM2, 2, RoundingMode.HALF_UP);
    }
}
