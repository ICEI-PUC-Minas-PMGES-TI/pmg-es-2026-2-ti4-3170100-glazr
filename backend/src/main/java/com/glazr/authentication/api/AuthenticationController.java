package com.glazr.authentication.api;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.authentication.application.AuthenticationService;
import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import com.glazr.authentication.infrastructure.SessionLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService service;
    private final SessionLoginService sessions;

    public AuthenticationController(AuthenticationService service, SessionLoginService sessions) {
        this.service = service;
        this.sessions = sessions;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) { return CsrfResponse.from(token); }

    @PostMapping("/login")
    AuthenticatedUser login(@Valid @RequestBody LoginRequest request,
                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthenticatedUser authenticated = service.authenticate(request.email(), request.password());
        sessions.login(authenticated, httpRequest, httpResponse);
        return authenticated;
    }

    @GetMapping("/me")
    AuthenticatedUser me(@AuthenticationPrincipal GlazrUserPrincipal principal) { return principal.user(); }
}
