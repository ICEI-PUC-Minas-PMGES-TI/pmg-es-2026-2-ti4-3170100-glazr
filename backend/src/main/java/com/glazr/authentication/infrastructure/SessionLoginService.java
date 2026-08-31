package com.glazr.authentication.infrastructure;

import com.glazr.authentication.application.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionLoginService {
    private final SecurityContextRepository contexts;

    public SessionLoginService(SecurityContextRepository contexts) { this.contexts = contexts; }

    public void login(AuthenticatedUser user, HttpServletRequest request, HttpServletResponse response) {
        var principal = new GlazrUserPrincipal(user);
        var authorities = user.permissions().stream().map(SimpleGrantedAuthority::new).toList();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }
}
