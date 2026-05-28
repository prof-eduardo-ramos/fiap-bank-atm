package com.fiap.bank.atm.domain.model;

import com.fiap.bank.atm.domain.exception.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Conta {
    private static final int MAX_TENTATIVAS_SENHA = 3;

    private final String numeroConta;
    private final String senha;
    private Dinheiro saldo;
    private final Dinheiro limiteSaqueDiario;
    private Dinheiro totalSacadoHoje;
    private boolean bloqueada;
    private int tentativasIncorretas;
    private final List<Transacao> transacoes;

    public Conta(String numeroConta, String senha, Dinheiro saldoInicial, Dinheiro limiteSaqueDiario) {
        this.numeroConta = Objects.requireNonNull(numeroConta, "Número da conta não pode ser nulo");
        this.senha = Objects.requireNonNull(senha, "Senha não pode ser nula");
        this.saldo = Objects.requireNonNull(saldoInicial, "Saldo inicial não pode ser nulo");
        this.limiteSaqueDiario = Objects.requireNonNull(limiteSaqueDiario, "Limite diário não pode ser nulo");
        this.totalSacadoHoje = Dinheiro.ZERO;
        this.bloqueada = false;
        this.tentativasIncorretas = 0;
        this.transacoes = new ArrayList<>();
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public Dinheiro getSaldo() {
        return saldo;
    }

    public Dinheiro getLimiteSaqueDiario() {
        return limiteSaqueDiario;
    }

    public Dinheiro getTotalSacadoHoje() {
        return totalSacadoHoje;
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    public int getTentativasIncorretas() {
        return tentativasIncorretas;
    }

    public List<Transacao> getTransacoes() {
        return Collections.unmodifiableList(transacoes);
    }

    public void autenticar(String tentativaSenha) {
        if (bloqueada) {
            throw new ContaBloqueadaException("Esta conta está bloqueada por excesso de tentativas de senha.");
        }

        if (!this.senha.equals(tentativaSenha)) {
            tentativasIncorretas++;
            if (tentativasIncorretas >= MAX_TENTATIVAS_SENHA) {
                bloqueada = true;
                throw new ContaBloqueadaException("Conta bloqueada após " + MAX_TENTATIVAS_SENHA + " tentativas incorretas.");
            }
            throw new SenhaInvalidaException("Senha incorreta. Tentativa " + tentativasIncorretas + " de " + MAX_TENTATIVAS_SENHA + ".");
        }

        tentativasIncorretas = 0; // Reseta as tentativas no sucesso
    }

    public void sacar(Dinheiro valorSaque) {
        if (bloqueada) {
            throw new ContaBloqueadaException("Operação não permitida: conta bloqueada.");
        }

        if (valorSaque.menorQue(Dinheiro.of(0.01))) {
            throw new IllegalArgumentException("O valor do saque deve ser maior que zero.");
        }

        if (valorSaque.maiorQue(saldo)) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar o saque. Saldo disponível: " + saldo);
        }

        Dinheiro projecaoSaque = totalSacadoHoje.mais(valorSaque);
        if (projecaoSaque.maiorQue(limiteSaqueDiario)) {
            throw new LimiteDiarioExcedidoException("Limite diário de saque excedido. Limite restante hoje: " 
                + limiteSaqueDiario.menos(totalSacadoHoje));
        }

        saldo = saldo.menos(valorSaque);
        totalSacadoHoje = totalSacadoHoje.mais(valorSaque);
        
        transacoes.add(new Transacao(TipoTransacao.SAQUE, valorSaque, "Saque eletrônico"));
    }

    public void depositar(Dinheiro valorDeposito) {
        if (bloqueada) {
            throw new ContaBloqueadaException("Operação não permitida: conta bloqueada.");
        }

        if (valorDeposito.menorQue(Dinheiro.of(0.01))) {
            throw new IllegalArgumentException("O valor do depósito deve ser maior que zero.");
        }

        saldo = saldo.mais(valorDeposito);
        transacoes.add(new Transacao(TipoTransacao.DEPOSITO, valorDeposito, "Depósito em dinheiro"));
    }

    public void transferir(Conta contaDestino, Dinheiro valorTransferencia) {
        if (bloqueada) {
            throw new ContaBloqueadaException("Operação não permitida: conta de origem bloqueada.");
        }

        if (contaDestino.isBloqueada()) {
            throw new ContaBloqueadaException("Operação não permitida: conta de destino está bloqueada.");
        }

        if (valorTransferencia.menorQue(Dinheiro.of(0.01))) {
            throw new IllegalArgumentException("O valor da transferência deve ser maior que zero.");
        }

        if (valorTransferencia.maiorQue(saldo)) {
            throw new SaldoInsuficienteException("Saldo insuficiente para transferência. Saldo disponível: " + saldo);
        }

        if (this.numeroConta.equals(contaDestino.getNumeroConta())) {
            throw new IllegalArgumentException("Não é possível realizar transferência para a mesma conta.");
        }

        // Debita a conta de origem
        this.saldo = this.saldo.menos(valorTransferencia);
        this.transacoes.add(new Transacao(
            TipoTransacao.TRANSFERENCIA_ENVIADA, 
            valorTransferencia, 
            "Transf. para Conta " + contaDestino.getNumeroConta()
        ));

        // Credita a conta de destino
        contaDestino.receberTransferencia(this, valorTransferencia);
    }

    private void receberTransferencia(Conta contaOrigem, Dinheiro valorTransferencia) {
        this.saldo = this.saldo.mais(valorTransferencia);
        this.transacoes.add(new Transacao(
            TipoTransacao.TRANSFERENCIA_RECEBIDA, 
            valorTransferencia, 
            "Transf. de Conta " + contaOrigem.getNumeroConta()
        ));
    }

    // Helper para dados de semente
    public void adicionarTransacaoAntiga(Transacao transacao) {
        this.transacoes.add(transacao);
    }
}
