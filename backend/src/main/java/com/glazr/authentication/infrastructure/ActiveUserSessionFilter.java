package com.glazr.authentication.infrastructure;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.authentication.application.AuthenticatedUserFactory;
import com.glazr.shared.api.SecurityProblemWriter;
import com.glazr.user.infrastructure.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ActiveUserSessionFilter extends OncePerRequestFilter {

    private final UserRepository users;
    private final SecurityProblemWriter problems;

    public ActiveUserSessionFilter(UserRepository users, SecurityProblemWriter problems) {
        this.users = users;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.isAuthenticated() && current.getPrincipal() instanceof GlazrUserPrincipal principal) {
            var user = users.findById(principal.user().id());
            if (user.isEmpty() || !user.get().isActive()) {
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                problems.write(request, response, HttpStatus.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED", "Autenticação necessária");
                return;
            }
            AuthenticatedUser refreshed = AuthenticatedUserFactory.from(user.get());
            GlazrUserPrincipal refreshedPrincipal = new GlazrUserPrincipal(refreshed);
            var authorities = refreshed.permissions().stream().map(SimpleGrantedAuthority::new).toList();
            UsernamePasswordAuthenticationToken replacement =
                    UsernamePasswordAuthenticationToken.authenticated(refreshedPrincipal, null, authorities);
            replacement.setDetails(current.getDetails());
            SecurityContextHolder.getContext().setAuthentication(replacement);
        }
        chain.doFilter(request, response);
    }
}
