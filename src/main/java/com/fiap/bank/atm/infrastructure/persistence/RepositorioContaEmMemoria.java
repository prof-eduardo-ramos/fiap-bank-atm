package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Conta;
import com.fiap.bank.atm.domain.model.Dinheiro;
import com.fiap.bank.atm.domain.model.Transacao;
import com.fiap.bank.atm.domain.model.TipoTransacao;
import com.fiap.bank.atm.domain.repository.RepositorioConta;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RepositorioContaEmMemoria implements RepositorioConta {
    private final Map<String, Conta> contas = new HashMap<>();

    public RepositorioContaEmMemoria() {
        carregarDadosSemente();
    }

    private void carregarDadosSemente() {
        // Conta 1
        Conta c1 = new Conta("12345", "1234", Dinheiro.of(5000.00), Dinheiro.of(1500.00));
        c1.adicionarTransacaoAntiga(new Transacao(LocalDateTime.now().minusDays(3), TipoTransacao.DEPOSITO, Dinheiro.of(2000.00), "Depósito em dinheiro"));
        c1.adicionarTransacaoAntiga(new Transacao(LocalDateTime.now().minusDays(2), TipoTransacao.TRANSFERENCIA_RECEBIDA, Dinheiro.of(500.00), "Transf. de Conta 67890"));
        c1.adicionarTransacaoAntiga(new Transacao(LocalDateTime.now().minusDays(1), TipoTransacao.SAQUE, Dinheiro.of(100.00), "Saque eletrônico"));
        contas.put(c1.getNumeroConta(), c1);

        // Conta 2
        Conta c2 = new Conta("67890", "5678", Dinheiro.of(1200.00), Dinheiro.of(1000.00));
        c2.adicionarTransacaoAntiga(new Transacao(LocalDateTime.now().minusDays(5), TipoTransacao.DEPOSITO, Dinheiro.of(1500.00), "Depósito inicial"));
        c2.adicionarTransacaoAntiga(new Transacao(LocalDateTime.now().minusDays(2), TipoTransacao.TRANSFERENCIA_ENVIADA, Dinheiro.of(500.00), "Transf. para Conta 12345"));
        contas.put(c2.getNumeroConta(), c2);

        // Conta 3
        Conta c3 = new Conta("99999", "9999", Dinheiro.of(50.00), Dinheiro.of(500.00));
        c3.adicionarTransacaoAntiga(new Transacao(LocalDateTime.now().minusDays(10), TipoTransacao.DEPOSITO, Dinheiro.of(50.00), "Abertura de conta"));
        contas.put(c3.getNumeroConta(), c3);
    }

    @Override
    public Optional<Conta> buscarPorNumero(String numeroConta) {
        return Optional.ofNullable(contas.get(numeroConta));
    }

    @Override
    public void salvar(Conta conta) {
        contas.put(conta.getNumeroConta(), conta);
    }
}
