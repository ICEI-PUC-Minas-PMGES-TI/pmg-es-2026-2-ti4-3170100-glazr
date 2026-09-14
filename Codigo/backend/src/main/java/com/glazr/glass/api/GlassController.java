package com.glazr.glass.api;

import com.glazr.glass.application.GlassService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/glasses")
public class GlassController {

    private final GlassService service;

    public GlassController(GlassService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GlassResponse create(@Valid @RequestBody CreateGlassRequest request) {
        return service.create(request);
    }

    @GetMapping
    public Page<GlassResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal thicknessMm,
            @RequestParam(required = false) String finish,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return service.list(query, thicknessMm, finish, active, pageable);
    }

    @GetMapping("/{id}")
    public GlassResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public GlassResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateGlassRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public GlassResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateGlassStatusRequest request) {
        return service.updateStatus(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
