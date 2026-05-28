package com.fiap.bank.atm.domain.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Transacao {
    private static final DateTimeFormatter FORMATADOR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LocalDateTime dataHora;
    private final TipoTransacao tipo;
    private final Dinheiro valor;
    private final String descricao;

    public Transacao(TipoTransacao tipo, Dinheiro valor, String descricao) {
        this.dataHora = LocalDateTime.now();
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
    }

    public Transacao(LocalDateTime dataHora, TipoTransacao tipo, Dinheiro valor, String descricao) {
        this.dataHora = dataHora;
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public Dinheiro getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDataHoraFormatada() {
        return dataHora.format(FORMATADOR);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (%s)", getDataHoraFormatada(), tipo.getDescricao(), valor, descricao);
    }
}
