package com.glazr.user.api;

import com.glazr.user.application.UserManagementService;
import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import com.glazr.invitation.api.InvitationResponse;
import com.glazr.invitation.application.InvitationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService service;
    private final InvitationService invitations;

    public UserController(UserManagementService service, InvitationService invitations) {
        this.service = service;
        this.invitations = invitations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest request,
                        @AuthenticationPrincipal GlazrUserPrincipal principal) {
        return service.create(request, principal.user().id());
    }

    @GetMapping
    Page<UserResponse> list(@RequestParam(required = false) String query,
                            @RequestParam(required = false) Boolean active, Pageable pageable) {
        return service.list(query, active, pageable);
    }

    @GetMapping("/{id}")
    UserResponse get(@PathVariable UUID id) { return service.get(id); }

    @PatchMapping("/{id}")
    UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    UserResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return service.updateStatus(id, request.active());
    }

    @GetMapping("/{id}/invitation")
    InvitationResponse invitation(@PathVariable UUID id) { return invitations.getForUser(id); }

    @PostMapping("/{id}/invitation/resend")
    InvitationResponse resendInvitation(@PathVariable UUID id,
                                        @AuthenticationPrincipal GlazrUserPrincipal principal) {
        return invitations.resend(id, principal.user().id());
    }

    @PostMapping("/{id}/invitation/revoke")
    InvitationResponse revokeInvitation(@PathVariable UUID id) { return invitations.revoke(id); }
}
