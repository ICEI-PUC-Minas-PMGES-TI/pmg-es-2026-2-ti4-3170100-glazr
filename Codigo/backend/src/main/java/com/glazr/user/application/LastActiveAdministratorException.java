package com.glazr.user.application;

public class LastActiveAdministratorException extends RuntimeException {
    public LastActiveAdministratorException() {
        super("A operação deixaria o sistema sem administrador ativo");
    }
}
