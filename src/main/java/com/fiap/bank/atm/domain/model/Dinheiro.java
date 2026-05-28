package com.fiap.bank.atm.domain.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public final class Dinheiro {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    private final BigDecimal valor;

    private Dinheiro(BigDecimal valor) {
        this.valor = valor.setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }

    public static Dinheiro of(double valor) {
        return new Dinheiro(BigDecimal.valueOf(valor));
    }

    public static Dinheiro of(BigDecimal valor) {
        Objects.requireNonNull(valor, "Valor não pode ser nulo");
        return new Dinheiro(valor);
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Dinheiro mais(Dinheiro outro) {
        Objects.requireNonNull(outro, "Outro dinheiro não pode ser nulo");
        return new Dinheiro(this.valor.add(outro.valor));
    }

    public Dinheiro menos(Dinheiro outro) {
        Objects.requireNonNull(outro, "Outro dinheiro não pode ser nulo");
        return new Dinheiro(this.valor.subtract(outro.valor));
    }

    public boolean maiorQue(Dinheiro outro) {
        Objects.requireNonNull(outro, "Outro dinheiro não pode ser nulo");
        return this.valor.compareTo(outro.valor) > 0;
    }

    public boolean maiorOuIgualA(Dinheiro outro) {
        Objects.requireNonNull(outro, "Outro dinheiro não pode ser nulo");
        return this.valor.compareTo(outro.valor) >= 0;
    }

    public boolean menorQue(Dinheiro outro) {
        Objects.requireNonNull(outro, "Outro dinheiro não pode ser nulo");
        return this.valor.compareTo(outro.valor) < 0;
    }

    public String formatar() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        return nf.format(valor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dinheiro dinheiro = (Dinheiro) o;
        return valor.compareTo(dinheiro.valor) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return formatar();
    }
}
