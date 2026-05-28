package com.fiap.bank.atm.domain.exception;

public class ContaBloqueadaException extends RuntimeException {
    public ContaBloqueadaException(String message) {
        super(message);
    }
}
