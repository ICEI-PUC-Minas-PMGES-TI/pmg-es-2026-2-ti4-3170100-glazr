package com.glazr.authentication.application;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("E-mail ou senha inválidos");
    }
}
