package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.domain.exception.SenhaInvalidaException;
import com.fiap.bank.atm.domain.model.Conta;
import com.fiap.bank.atm.domain.model.Dinheiro;
import com.fiap.bank.atm.domain.model.Transacao;
import com.fiap.bank.atm.domain.repository.RepositorioConta;
import java.util.List;
import java.util.Optional;

public class ServicoCaixaEletronico {
    private final RepositorioConta repositorioConta;
    private Conta contaAutenticada;

    public ServicoCaixaEletronico(RepositorioConta repositorioConta) {
        this.repositorioConta = repositorioConta;
    }

    public Conta autenticar(String numeroConta, String senha) {
        Optional<Conta> contaOpt = repositorioConta.buscarPorNumero(numeroConta);
        
        if (contaOpt.isEmpty()) {
            throw new SenhaInvalidaException("Conta não encontrada.");
        }

        Conta conta = contaOpt.get();
        try {
            conta.autenticar(senha);
            contaAutenticada = conta;
            return conta;
        } catch (RuntimeException e) {
            repositorioConta.salvar(conta); // Salva para persistir tentativas/bloqueio
            throw e;
        }
    }

    public void sacar(double valor) {
        garantirAutenticado();
        contaAutenticada.sacar(Dinheiro.of(valor));
        repositorioConta.salvar(contaAutenticada);
    }

    public void depositar(double valor) {
        garantirAutenticado();
        contaAutenticada.depositar(Dinheiro.of(valor));
        repositorioConta.salvar(contaAutenticada);
    }

    public void transferir(String numeroContaDestino, double valor) {
        garantirAutenticado();
        
        Optional<Conta> destinoOpt = repositorioConta.buscarPorNumero(numeroContaDestino);
        if (destinoOpt.isEmpty()) {
            throw new IllegalArgumentException("Conta de destino não encontrada.");
        }

        Conta contaDestino = destinoOpt.get();
        contaAutenticada.transferir(contaDestino, Dinheiro.of(valor));
        
        repositorioConta.salvar(contaAutenticada);
        repositorioConta.salvar(contaDestino);
    }

    public Dinheiro obterSaldo() {
        garantirAutenticado();
        return contaAutenticada.getSaldo();
    }

    public List<Transacao> obterExtrato() {
        garantirAutenticado();
        return contaAutenticada.getTransacoes();
    }

    public void logout() {
        contaAutenticada = null;
    }

    public Conta getContaAutenticada() {
        return contaAutenticada;
    }

    public boolean estaAutenticado() {
        return contaAutenticada != null;
    }

    private void garantirAutenticado() {
        if (!estaAutenticado()) {
            throw new IllegalStateException("Nenhum usuário está autenticado no momento.");
        }
    }
}
