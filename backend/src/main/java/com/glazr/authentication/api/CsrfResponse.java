package com.glazr.authentication.api;

import org.springframework.security.web.csrf.CsrfToken;

public record CsrfResponse(String headerName, String parameterName, String token) {
    public static CsrfResponse from(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }
}
