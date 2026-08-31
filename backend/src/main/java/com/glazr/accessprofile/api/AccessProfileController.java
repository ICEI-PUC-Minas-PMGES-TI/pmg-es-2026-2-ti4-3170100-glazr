package com.glazr.accessprofile.api;

import com.glazr.accessprofile.application.AccessProfileService;
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
@RequestMapping("/api/v1/access-profiles")
public class AccessProfileController {

    private final AccessProfileService service;

    public AccessProfileController(AccessProfileService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AccessProfileResponse create(@Valid @RequestBody AccessProfileRequest request) { return service.create(request); }

    @GetMapping
    Page<AccessProfileResponse> list(@RequestParam(required = false) String query, Pageable pageable) {
        return service.list(query, pageable);
    }

    @GetMapping("/{id}")
    AccessProfileResponse get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    AccessProfileResponse replace(@PathVariable UUID id, @Valid @RequestBody AccessProfileRequest request) {
        return service.replace(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) { service.delete(id); }
}
