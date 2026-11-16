package com.glazr.authentication.infrastructure;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.shared.api.SecurityProblemWriter;
import com.glazr.user.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ActiveUserSessionFilterTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SecurityProblemWriter problems = mock(SecurityProblemWriter.class);
    private final ActiveUserSessionFilter filter = new ActiveUserSessionFilter(users, problems);

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void retainsSessionPrincipalAndAuthenticationMethodsForActiveUser() throws Exception {
        var user = authenticate();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        when(users.existsByIdAndActiveTrue(user.id())).thenReturn(true);
        var chain = mock(FilterChain.class);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
        verify(users).existsByIdAndActiveTrue(user.id());
        verifyNoMoreInteractions(users);
        verifyNoInteractions(problems);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidatesSessionWhenUserIsInactiveOrMissing() throws Exception {
        authenticate();
        var request = new MockHttpServletRequest();
        var session = (org.springframework.mock.web.MockHttpSession) request.getSession();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
        verify(problems).write(eq(request), any(), eq(org.springframework.http.HttpStatus.UNAUTHORIZED),
                eq("AUTHENTICATION_REQUIRED"), eq("Autenticação necessária"));
    }

    private AuthenticatedUser authenticate() {
        var user = new AuthenticatedUser(UUID.randomUUID(), "user@example.com", "User", true,
                "STANDARD", List.of(), List.of(), List.of("GOOGLE", "PASSWORD"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(new GlazrUserPrincipal(user), null, List.of()));
        return user;
    }
}
