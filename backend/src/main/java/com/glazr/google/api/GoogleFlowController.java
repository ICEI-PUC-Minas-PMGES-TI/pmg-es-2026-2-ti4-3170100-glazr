package com.glazr.google.api;

import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import com.glazr.google.infrastructure.GoogleFlowSession;
import com.glazr.invitation.api.InvitationTokenRequest;
import com.glazr.invitation.application.InvitationService;
import com.glazr.shared.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GoogleFlowController {
    private final InvitationService invitations;
    private final boolean enabled;

    public GoogleFlowController(InvitationService invitations,
                                @Value("${glazr.google-auth-enabled:false}") boolean enabled) {
        this.invitations = invitations;
        this.enabled = enabled;
    }

    @GetMapping("/auth/config")
    AuthenticationConfigurationResponse configuration() {
        return new AuthenticationConfigurationResponse(enabled);
    }

    @PostMapping("/auth/google/start")
    GoogleStartResponse startLogin(HttpServletRequest request) {
        requireEnabled();
        clearFlow(request);
        return GoogleStartResponse.google();
    }

    @PostMapping("/auth/google/link/start")
    GoogleStartResponse startLink(@AuthenticationPrincipal GlazrUserPrincipal principal,
                                  HttpServletRequest request) {
        requireEnabled();
        clearFlow(request);
        request.getSession(true).setAttribute(GoogleFlowSession.EXPECTED_USER_ID, principal.user().id());
        return GoogleStartResponse.google();
    }

    @PostMapping("/invitations/google/start")
    GoogleStartResponse startInvitation(@Valid @RequestBody InvitationTokenRequest body,
                                        HttpServletRequest request) {
        requireEnabled();
        var invitationId = invitations.prepareGoogle(body.token());
        clearFlow(request);
        request.getSession(true).setAttribute(GoogleFlowSession.INVITATION_ID, invitationId);
        return GoogleStartResponse.google();
    }

    private void clearFlow(HttpServletRequest request) {
        request.getSession(true).removeAttribute(GoogleFlowSession.INVITATION_ID);
        request.getSession().removeAttribute(GoogleFlowSession.EXPECTED_USER_ID);
    }

    private void requireEnabled() {
        if (!enabled) throw new ApplicationException(HttpStatus.NOT_FOUND, "GOOGLE_AUTH_DISABLED",
                "Autenticação Google não está habilitada");
    }
}
