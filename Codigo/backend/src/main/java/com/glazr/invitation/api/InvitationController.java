package com.glazr.invitation.api;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.authentication.infrastructure.SessionLoginService;
import com.glazr.invitation.application.InvitationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {
    private final InvitationService invitations;
    private final SessionLoginService sessions;

    public InvitationController(InvitationService invitations, SessionLoginService sessions) {
        this.invitations = invitations;
        this.sessions = sessions;
    }

    @PostMapping("/inspect")
    InvitationInspectionResponse inspect(@Valid @RequestBody InvitationTokenRequest request) {
        return invitations.inspect(request.token());
    }

    @PostMapping("/accept-with-password")
    AuthenticatedUser accept(@Valid @RequestBody AcceptInvitationRequest request,
                             HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthenticatedUser user = invitations.acceptWithPassword(request.token(), request.password());
        sessions.login(user, httpRequest, httpResponse);
        return user;
    }
}
