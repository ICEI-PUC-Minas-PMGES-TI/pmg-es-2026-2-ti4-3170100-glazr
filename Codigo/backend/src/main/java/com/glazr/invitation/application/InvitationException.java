package com.glazr.invitation.application;

import com.glazr.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvitationException extends ApplicationException {
    public InvitationException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
