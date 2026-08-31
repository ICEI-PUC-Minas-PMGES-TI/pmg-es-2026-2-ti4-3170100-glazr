package com.glazr.password.api;

import com.glazr.authentication.application.AuthenticatedUser;
import com.glazr.authentication.infrastructure.GlazrUserPrincipal;
import com.glazr.password.application.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordControllerTest {
    @Test
    void resetInvalidatesCurrentHttpSessionWhenItBelongsToTheTargetUser() {
        UUID userId = UUID.randomUUID();
        PasswordService passwords = mock(PasswordService.class);
        when(passwords.completeReset("token", "nova frase senha segura")).thenReturn(userId);
        var principal = new GlazrUserPrincipal(new AuthenticatedUser(
                userId, "person@example.com", "Person", true, "EMPLOYEE",
                List.of(), List.of(), List.of("PASSWORD")));
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) httpRequest.getSession();

        new PasswordController(passwords).completeReset(
                new PasswordResetCompleteRequest("token", "nova frase senha segura"), principal, httpRequest);

        verify(passwords).invalidateAllSessions(userId);
        assertThat(session.isInvalid()).isTrue();
    }
}
