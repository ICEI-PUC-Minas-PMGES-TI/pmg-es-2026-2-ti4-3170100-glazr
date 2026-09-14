package com.glazr.kit.api;

import com.glazr.kit.application.KitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/kits")
public class KitController {

    private final KitService service;

    public KitController(KitService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    KitResponse create(@Valid @RequestBody KitRequest request) { return service.create(request); }

    @GetMapping
    Page<KitResponse> list(@RequestParam(required = false) String query, Pageable pageable) {
        return service.list(query, pageable);
    }

    @GetMapping("/{id}")
    KitResponse get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    KitResponse replace(@PathVariable UUID id, @Valid @RequestBody KitRequest request) {
        return service.replace(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) { service.delete(id); }
}