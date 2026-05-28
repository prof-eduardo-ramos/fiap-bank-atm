package com.fiap.bank.atm.domain.exception;

public class LimiteDiarioExcedidoException extends RuntimeException {
    public LimiteDiarioExcedidoException(String message) {
        super(message);
    }
}
