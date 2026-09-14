package com.glazr.inventory.api;

import com.glazr.inventory.application.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/glasses")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public Page<GlassStockSummaryResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/alerts")
    public List<GlassStockSummaryResponse> listAlerts() {
        return service.listAlerts();
    }

    @GetMapping("/{glassId}")
    public GlassStockSummaryResponse getSummary(@PathVariable UUID glassId) {
        return service.getSummary(glassId);
    }

    @GetMapping("/{glassId}/sheets")
    public Page<GlassSheetResponse> listSheets(@PathVariable UUID glassId, Pageable pageable) {
        return service.listSheets(glassId, pageable);
    }

    @GetMapping("/{glassId}/movements")
    public Page<StockMovementResponse> listMovements(@PathVariable UUID glassId, Pageable pageable) {
        return service.listMovements(glassId, pageable);
    }

    @PutMapping("/{glassId}/minimum")
    public GlassStockSummaryResponse updateMinimum(
            @PathVariable UUID glassId,
            @Valid @RequestBody UpdateStockMinimumRequest request) {
        return service.updateMinimum(glassId, request.minimumAreaM2());
    }
}
