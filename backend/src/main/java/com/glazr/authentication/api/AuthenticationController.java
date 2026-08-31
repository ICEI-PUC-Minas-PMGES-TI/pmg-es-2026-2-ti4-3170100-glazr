package com.glazr.authentication.api;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.authentication.application.AuthenticationService;
import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
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
    private final SecurityContextRepository securityContextRepository;

    public AuthenticationController(AuthenticationService service,
                                    SecurityContextRepository securityContextRepository) {
        this.service = service;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) { return CsrfResponse.from(token); }

    @PostMapping("/login")
    AuthenticatedUser login(@Valid @RequestBody LoginRequest request,
                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthenticatedUser authenticated = service.authenticate(request.email(), request.password());
        GlazrUserPrincipal principal = new GlazrUserPrincipal(authenticated);
        var authorities = authenticated.permissions().stream().map(SimpleGrantedAuthority::new).toList();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return authenticated;
    }

    @GetMapping("/me")
    AuthenticatedUser me(@AuthenticationPrincipal GlazrUserPrincipal principal) { return principal.user(); }
}
