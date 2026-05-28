package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.Conta;
import java.util.Optional;

public interface RepositorioConta {
    Optional<Conta> buscarPorNumero(String numeroConta);
    void salvar(Conta conta);
}
