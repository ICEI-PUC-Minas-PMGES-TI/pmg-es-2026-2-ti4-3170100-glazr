package com.glazr.google.infrastructure;

import com.glazr.authentication.infrastructure.SessionLoginService;
import com.glazr.google.application.GoogleIdentityService;
import com.glazr.mail.infrastructure.PublicBaseUrl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class GoogleAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final GoogleIdentityService identities;
    private final SessionLoginService sessions;
    private final String frontendOrigin;

    public GoogleAuthenticationSuccessHandler(GoogleIdentityService identities, SessionLoginService sessions,
                                              @Value("${glazr.public-base-url}") String publicBaseUrl) {
        this.identities = identities;
        this.sessions = sessions;
        this.frontendOrigin = new PublicBaseUrl(publicBaseUrl).origin();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OidcUser oidc = (OidcUser) authentication.getPrincipal();
        UUID invitationId = sessionUuid(request, GoogleFlowSession.INVITATION_ID);
        UUID expectedUserId = sessionUuid(request, GoogleFlowSession.EXPECTED_USER_ID);
        request.getSession().removeAttribute(GoogleFlowSession.INVITATION_ID);
        request.getSession().removeAttribute(GoogleFlowSession.EXPECTED_USER_ID);
        Boolean verified = oidc.getClaim("email_verified");
        try {
            var user = identities.authenticate(oidc.getSubject(), oidc.getEmail(), Boolean.TRUE.equals(verified),
                    invitationId, expectedUserId);
            sessions.login(user, request, response);
            response.sendRedirect(frontendOrigin + "/");
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            response.sendRedirect(frontendOrigin + "/login?googleError=GOOGLE_AUTHENTICATION_FAILED");
        }
    }

    private UUID sessionUuid(HttpServletRequest request, String key) {
        Object value = request.getSession().getAttribute(key);
        return value instanceof UUID id ? id : null;
    }
}
