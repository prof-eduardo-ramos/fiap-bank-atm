package com.fiap.bank.atm.domain.model;

public enum TipoTransacao {
    SAQUE("Saque"),
    DEPOSITO("Depósito"),
    TRANSFERENCIA_ENVIADA("Transf. Enviada"),
    TRANSFERENCIA_RECEBIDA("Transf. Recebida");

    private final String descricao;

    TipoTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
