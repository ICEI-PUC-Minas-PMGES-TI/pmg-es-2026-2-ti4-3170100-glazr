package com.glazr.password.api;

import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import com.glazr.password.application.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class PasswordController {
    private final PasswordService passwords;

    public PasswordController(PasswordService passwords) { this.passwords = passwords; }

    @PostMapping("/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void change(@Valid @RequestBody PasswordChangeRequest request,
                @AuthenticationPrincipal GlazrUserPrincipal principal, HttpServletRequest httpRequest) {
        passwords.change(principal.user().id(), request.currentPassword(), request.newPassword());
        passwords.invalidateOtherSessions(principal.user().id(), httpRequest.getSession(false).getId());
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void requestReset(@Valid @RequestBody PasswordResetRequest request) {
        passwords.requestReset(request.email());
    }

    @PostMapping("/password-reset/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void completeReset(@Valid @RequestBody PasswordResetCompleteRequest request,
                       @AuthenticationPrincipal GlazrUserPrincipal principal,
                       HttpServletRequest httpRequest) {
        var userId = passwords.completeReset(request.token(), request.newPassword());
        passwords.invalidateAllSessions(userId);
        HttpSession currentSession = httpRequest.getSession(false);
        if (principal != null && principal.user().id().equals(userId) && currentSession != null) {
            currentSession.invalidate();
            SecurityContextHolder.clearContext();
        }
    }
}
