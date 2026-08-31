package com.glazr.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resource) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", resource + " não encontrado");
    }
}
