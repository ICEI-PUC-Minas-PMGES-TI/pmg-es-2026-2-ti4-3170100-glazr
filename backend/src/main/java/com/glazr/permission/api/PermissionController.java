package com.glazr.permission.api;

import com.glazr.permission.application.PermissionQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {
    private final PermissionQueryService service;

    public PermissionController(PermissionQueryService service) { this.service = service; }

    @GetMapping
    List<PermissionResponse> list() { return service.list(); }
}
