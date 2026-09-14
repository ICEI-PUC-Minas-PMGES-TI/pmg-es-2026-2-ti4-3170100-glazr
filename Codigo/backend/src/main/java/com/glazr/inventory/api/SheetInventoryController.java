package com.glazr.inventory.api;

import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import com.glazr.inventory.application.InventoryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/sheets")
public class SheetInventoryController {

    private final InventoryService service;

    public SheetInventoryController(InventoryService service) {
        this.service = service;
    }

    @PatchMapping("/{sheetId}/adjustment")
    public GlassSheetResponse adjust(
            @PathVariable UUID sheetId,
            @Valid @RequestBody UpdateSheetStockRequest request,
            @AuthenticationPrincipal GlazrUserPrincipal principal) {
        return service.adjust(sheetId, request, principal.user().id());
    }

    @PostMapping("/{sheetId}/cuts")
    public ManualCutResponse cut(
            @PathVariable UUID sheetId,
            @Valid @RequestBody UpdateSheetStockRequest.Cut request,
            @AuthenticationPrincipal GlazrUserPrincipal principal) {
        return service.cut(sheetId, request, principal.user().id());
    }
}
