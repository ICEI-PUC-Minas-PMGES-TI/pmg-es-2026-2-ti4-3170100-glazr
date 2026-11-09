package com.glazr.google.infrastructure;

import com.glazr.mail.infrastructure.PublicBaseUrl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final String frontendOrigin;

    public GoogleAuthenticationFailureHandler(@Value("${glazr.public-base-url}") String publicBaseUrl) {
        this.frontendOrigin = new PublicBaseUrl(publicBaseUrl).origin();
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        response.sendRedirect(frontendOrigin + "/login?googleError=GOOGLE_AUTHENTICATION_FAILED");
    }
}
