package com.fiap.bank.atm.presentation;

import com.formdev.flatlaf.FlatDarkLaf;
import com.fiap.bank.atm.application.service.ServicoCaixaEletronico;
import com.fiap.bank.atm.domain.exception.ContaBloqueadaException;
import com.fiap.bank.atm.domain.exception.LimiteDiarioExcedidoException;
import com.fiap.bank.atm.domain.exception.SaldoInsuficienteException;
import com.fiap.bank.atm.domain.exception.SenhaInvalidaException;
import com.fiap.bank.atm.domain.model.Conta;
import com.fiap.bank.atm.domain.model.Transacao;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FormularioCaixaEletronico extends javax.swing.JFrame {

    private final ServicoCaixaEletronico servicoCaixaEletronico;
    private EstadoTela estadoAtual;
    private final StringBuilder bufferEntrada;
    private String numeroContaDestino;
    private String mensagemErro;

    // Timers de animação
    private Timer timerCardLed;
    private boolean ledCardAceso = true;
    private Timer timerAnimacaoSaque;
    private Timer timerAnimacaoImpressao;
    private Timer timerSucesso;

    // Comprovante Virtual
    private JDialog dialogoRecibo;
    private JTextArea txtPapelRecibo;

    public FormularioCaixaEletronico(ServicoCaixaEletronico servicoCaixaEletronico) {
        this.servicoCaixaEletronico = servicoCaixaEletronico;
        this.bufferEntrada = new StringBuilder();
        this.estadoAtual = EstadoTela.BEM_VINDO;

        // Inicializa o FlatLaf Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Falha ao inicializar o FlatLaf Look and Feel");
        }

        initComponents();
        configurarEstilosPersonalizados();
        configurarListeners();
        configurarTimers();
        configurarInterceptacaoTeclado();

        atualizarTela();
    }

    private void configurarEstilosPersonalizados() {
        // Estilização da tela (CRT/LCD look)
        jPanelScreen.setBackground(new Color(11, 18, 28));
        jPanelScreenHeader.setBackground(new Color(11, 18, 28));
        jPanelScreenCenter.setBackground(new Color(11, 18, 28));
        jPanelScreenLeftLabels.setBackground(new Color(11, 18, 28));
        jPanelScreenRightLabels.setBackground(new Color(11, 18, 28));

        lblScreenHeader.setForeground(new Color(254, 240, 138));
        lblScreenStatus.setForeground(new Color(241, 245, 249));
        lblScreenInput.setForeground(new Color(56, 189, 248));
        lblScreenMessage.setForeground(new Color(234, 113, 113));

        // Botões físicos laterais
        JButton[] botoesLaterais = { btnLeft1, btnLeft2, btnLeft3, btnRight1, btnRight2, btnRight3 };
        for (JButton btn : botoesLaterais) {
            btn.setBackground(new Color(51, 65, 85));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(new LineBorder(new Color(71, 85, 105), 2));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        // Teclado numérico
        JButton[] botoesNumericos = { btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0, btnBlank, btnC };
        for (JButton btn : botoesNumericos) {
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            if (btn == btnC) {
                btn.setBackground(new Color(189, 58, 58));
                btn.setForeground(Color.WHITE);
                btn.setBorder(new LineBorder(new Color(220, 80, 80), 2));
            } else if (btn == btnBlank) {
                btn.setBackground(new Color(30, 41, 59));
                btn.setForeground(new Color(148, 163, 184));
                btn.setBorder(new LineBorder(new Color(71, 85, 105), 1));
            } else {
                btn.setBackground(new Color(30, 41, 59));
                btn.setForeground(new Color(241, 245, 249));
                btn.setBorder(new LineBorder(new Color(71, 85, 105), 2));
            }
        }

        // Contêineres de periféricos
        cardSlotContainer.setBackground(new Color(18, 27, 38));
        receiptPrinterContainer.setBackground(new Color(18, 27, 38));
        cashDispenserContainer.setBackground(new Color(18, 27, 38));
    }

    private void configurarTimers() {
        // LED do Cartão piscando no WELCOME
        timerCardLed = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoAtual == EstadoTela.BEM_VINDO) {
                    ledCardAceso = !ledCardAceso;
                    if (ledCardAceso) {
                        lblCardIndicatorLed.setForeground(new Color(80, 200, 80));
                        lblCardIndicatorLed.setText("● INSERIR CARTÃO");
                    } else {
                        lblCardIndicatorLed.setForeground(new Color(30, 70, 30));
                        lblCardIndicatorLed.setText("  INSERIR CARTÃO");
                    }
                }
            }
        });
        timerCardLed.start();

        // Temporizador para dispensar dinheiro
        timerAnimacaoSaque = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timerAnimacaoSaque.stop();
                lblCashDispenserStatus.setText("FECHADO");
                lblCashDispenserStatus.setForeground(Color.GRAY);
                cashDispenserContainer.setBackground(new Color(18, 27, 38));

                estadoAtual = EstadoTela.SUCESSO;
                atualizarTela();
                timerSucesso.start();
            }
        });

        // Temporizador para impressão
        timerAnimacaoImpressao = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timerAnimacaoImpressao.stop();
                lblPrinterStatus.setText("PRONTA");
                lblPrinterStatus.setForeground(Color.LIGHT_GRAY);
                receiptPrinterContainer.setBackground(new Color(18, 27, 38));

                exibirComprovanteVirtual();

                estadoAtual = EstadoTela.SUCESSO;
                atualizarTela();
                timerSucesso.start();
            }
        });

        // Temporizador da tela de sucesso (volta para o menu ou tela inicial)
        timerSucesso = new Timer(4000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timerSucesso.stop();
                if (servicoCaixaEletronico.estaAutenticado()) {
                    estadoAtual = EstadoTela.MENU_PRINCIPAL;
                } else {
                    estadoAtual = EstadoTela.BEM_VINDO;
                }
                bufferEntrada.setLength(0);
                atualizarTela();
            }
        });
    }

    private void configurarInterceptacaoTeclado() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                char keyChar = e.getKeyChar();
                int keyCode = e.getKeyCode();

                if (Character.isDigit(keyChar)) {
                    processarEntradaNumerica(String.valueOf(keyChar));
                    return true;
                } else if (keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_ESCAPE) {
                    processarLimparOuCancelar();
                    return true;
                } else if (keyCode == KeyEvent.VK_ENTER) {
                    processarConfirmar();
                    return true;
                }
            }
            return false;
        });
    }

    private void configurarListeners() {
        JButton[] botoesNumericos = { btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0 };
        for (JButton btn : botoesNumericos) {
            btn.addActionListener(e -> processarEntradaNumerica(btn.getText()));
        }

        btnC.addActionListener(e -> processarLimparOuCancelar());
        btnBlank.addActionListener(e -> processarConfirmar());

        btnLeft1.addActionListener(e -> processarBotaoLateral("L1"));
        btnLeft2.addActionListener(e -> processarBotaoLateral("L2"));
        btnLeft3.addActionListener(e -> processarBotaoLateral("L3"));
        btnRight1.addActionListener(e -> processarBotaoLateral("R1"));
        btnRight2.addActionListener(e -> processarBotaoLateral("R2"));
        btnRight3.addActionListener(e -> processarBotaoLateral("R3"));
    }

    private void processarEntradaNumerica(String texto) {
        if (estaEmEstadoDeAnimacao())
            return;

        if (estadoAtual == EstadoTela.BEM_VINDO) {
            if (bufferEntrada.length() < 10) {
                bufferEntrada.append(texto);
            }
        } else if (estadoAtual == EstadoTela.DIGITAR_SENHA) {
            if (bufferEntrada.length() < 4) {
                bufferEntrada.append(texto);
            }
        } else if (estadoAtual == EstadoTela.SAQUE_PERSONALIZADO || estadoAtual == EstadoTela.DIGITAR_DEPOSITO
                || estadoAtual == EstadoTela.TRANSFERENCIA_VALOR) {
            if (bufferEntrada.length() < 7) {
                bufferEntrada.append(texto);
            }
        } else if (estadoAtual == EstadoTela.TRANSFERENCIA_CONTA) {
            if (bufferEntrada.length() < 10) {
                bufferEntrada.append(texto);
            }
        }
        atualizarTela();
    }

    private void processarLimparOuCancelar() {
        if (estaEmEstadoDeAnimacao())
            return;

        if (bufferEntrada.length() > 0) {
            bufferEntrada.setLength(bufferEntrada.length() - 1);
            atualizarTela();
        } else {
            switch (estadoAtual) {
                case DIGITAR_SENHA:
                    servicoCaixaEletronico.logout();
                    estadoAtual = EstadoTela.BEM_VINDO;
                    break;
                case SELECIONAR_SAQUE:
                case DIGITAR_DEPOSITO:
                case TRANSFERENCIA_CONTA:
                case EXIBIR_SALDO:
                case EXIBIR_EXTRATO:
                    estadoAtual = EstadoTela.MENU_PRINCIPAL;
                    break;
                case SAQUE_PERSONALIZADO:
                    estadoAtual = EstadoTela.SELECIONAR_SAQUE;
                    break;
                case TRANSFERENCIA_VALOR:
                    estadoAtual = EstadoTela.TRANSFERENCIA_CONTA;
                    break;
                case MENU_PRINCIPAL:
                    servicoCaixaEletronico.logout();
                    estadoAtual = EstadoTela.BEM_VINDO;
                    break;
                case ERRO:
                    if (servicoCaixaEletronico.estaAutenticado()) {
                        estadoAtual = EstadoTela.MENU_PRINCIPAL;
                    } else {
                        estadoAtual = EstadoTela.BEM_VINDO;
                    }
                    break;
                default:
                    break;
            }
            bufferEntrada.setLength(0);
            atualizarTela();
        }
    }

    private void processarConfirmar() {
        if (estaEmEstadoDeAnimacao())
            return;

        try {
            switch (estadoAtual) {
                case BEM_VINDO:
                    if (bufferEntrada.length() > 0) {
                        numeroContaDestino = bufferEntrada.toString();
                        bufferEntrada.setLength(0);
                        estadoAtual = EstadoTela.DIGITAR_SENHA;
                    } else {
                        mensagemErro = "DIGITE O NÚMERO DA CONTA";
                        estadoAtual = EstadoTela.ERRO;
                    }
                    break;

                case DIGITAR_SENHA:
                    if (bufferEntrada.length() == 4) {
                        String senha = bufferEntrada.toString();
                        bufferEntrada.setLength(0);
                        servicoCaixaEletronico.autenticar(numeroContaDestino, senha);
                        estadoAtual = EstadoTela.MENU_PRINCIPAL;
                    } else {
                        mensagemErro = "DIGITE A SENHA DE 4 DÍGITOS";
                        estadoAtual = EstadoTela.ERRO;
                    }
                    break;

                case SAQUE_PERSONALIZADO:
                    if (bufferEntrada.length() > 0) {
                        double val = Double.parseDouble(bufferEntrada.toString());
                        bufferEntrada.setLength(0);
                        dispararSaque(val);
                    } else {
                        mensagemErro = "DIGITE UM VALOR VÁLIDO";
                        estadoAtual = EstadoTela.ERRO;
                    }
                    break;

                case DIGITAR_DEPOSITO:
                    if (bufferEntrada.length() > 0) {
                        double val = Double.parseDouble(bufferEntrada.toString());
                        bufferEntrada.setLength(0);
                        dispararDeposito(val);
                    } else {
                        mensagemErro = "DIGITE UM VALOR VÁLIDO";
                        estadoAtual = EstadoTela.ERRO;
                    }
                    break;

                case TRANSFERENCIA_CONTA:
                    if (bufferEntrada.length() > 0) {
                        numeroContaDestino = bufferEntrada.toString();
                        bufferEntrada.setLength(0);
                        estadoAtual = EstadoTela.TRANSFERENCIA_VALOR;
                    } else {
                        mensagemErro = "INSIRA A CONTA DESTINO";
                        estadoAtual = EstadoTela.ERRO;
                    }
                    break;

                case TRANSFERENCIA_VALOR:
                    if (bufferEntrada.length() > 0) {
                        double val = Double.parseDouble(bufferEntrada.toString());
                        bufferEntrada.setLength(0);
                        servicoCaixaEletronico.transferir(numeroContaDestino, val);
                        estadoAtual = EstadoTela.SUCESSO;
                    } else {
                        mensagemErro = "DIGITE UM VALOR VÁLIDO";
                        estadoAtual = EstadoTela.ERRO;
                    }
                    break;

                default:
                    break;
            }
        } catch (ContaBloqueadaException ex) {
            mensagemErro = "CONTA BLOQUEADA!";
            estadoAtual = EstadoTela.ERRO;
        } catch (SenhaInvalidaException ex) {
            mensagemErro = "SENHA INCORRETA!";
            estadoAtual = EstadoTela.ERRO;
        } catch (SaldoInsuficienteException ex) {
            mensagemErro = "SALDO INSUFICIENTE!";
            estadoAtual = EstadoTela.ERRO;
        } catch (LimiteDiarioExcedidoException ex) {
            mensagemErro = "LIMITE DIÁRIO EXCEDIDO!";
            estadoAtual = EstadoTela.ERRO;
        } catch (IllegalArgumentException ex) {
            mensagemErro = ex.getMessage().toUpperCase();
            estadoAtual = EstadoTela.ERRO;
        } catch (Exception ex) {
            mensagemErro = "ERRO NO SISTEMA";
            estadoAtual = EstadoTela.ERRO;
        }
        atualizarTela();
    }

    private void processarBotaoLateral(String botaoId) {
        if (estaEmEstadoDeAnimacao())
            return;

        switch (estadoAtual) {
            case MENU_PRINCIPAL:
                if (botaoId.equals("L1")) {
                    estadoAtual = EstadoTela.SELECIONAR_SAQUE;
                } else if (botaoId.equals("L2")) {
                    estadoAtual = EstadoTela.DIGITAR_DEPOSITO;
                } else if (botaoId.equals("L3")) {
                    estadoAtual = EstadoTela.TRANSFERENCIA_CONTA;
                } else if (botaoId.equals("R1")) {
                    estadoAtual = EstadoTela.EXIBIR_SALDO;
                } else if (botaoId.equals("R2")) {
                    dispararImpressaoExtrato();
                } else if (botaoId.equals("R3")) {
                    servicoCaixaEletronico.logout();
                    estadoAtual = EstadoTela.BEM_VINDO;
                }
                break;

            case SELECIONAR_SAQUE:
                if (botaoId.equals("L1")) {
                    dispararSaque(20);
                } else if (botaoId.equals("L2")) {
                    dispararSaque(50);
                } else if (botaoId.equals("L3")) {
                    dispararSaque(100);
                } else if (botaoId.equals("R1")) {
                    dispararSaque(200);
                } else if (botaoId.equals("R2")) {
                    dispararSaque(500);
                } else if (botaoId.equals("R3")) {
                    estadoAtual = EstadoTela.SAQUE_PERSONALIZADO;
                }
                break;

            case EXIBIR_SALDO:
            case EXIBIR_EXTRATO:
                if (botaoId.equals("R3")) {
                    estadoAtual = EstadoTela.MENU_PRINCIPAL;
                }
                break;

            default:
                break;
        }
        bufferEntrada.setLength(0);
        atualizarTela();
    }

    private boolean estaEmEstadoDeAnimacao() {
        return estadoAtual == EstadoTela.ANIMACAO_SAQUE ||
                estadoAtual == EstadoTela.ANIMACAO_IMPRESSAO ||
                estadoAtual == EstadoTela.ANIMACAO_DEPOSITO;
    }

    private void dispararSaque(double valor) {
        try {
            servicoCaixaEletronico.sacar(valor);
            estadoAtual = EstadoTela.ANIMACAO_SAQUE;
            atualizarTela();

            lblCashDispenserStatus.setText("RETIRE SUAS CÉDULAS");
            lblCashDispenserStatus.setForeground(new Color(50, 255, 50));
            cashDispenserContainer.setBackground(new Color(20, 80, 20));

            timerAnimacaoSaque.start();
        } catch (Exception ex) {
            mensagemErro = ex.getMessage().toUpperCase();
            estadoAtual = EstadoTela.ERRO;
            atualizarTela();
        }
    }

    private void dispararDeposito(double valor) {
        try {
            servicoCaixaEletronico.depositar(valor);
            estadoAtual = EstadoTela.ANIMACAO_DEPOSITO;
            atualizarTela();

            Timer depTimer = new Timer(2000, e -> {
                estadoAtual = EstadoTela.SUCESSO;
                atualizarTela();
                timerSucesso.start();
            });
            depTimer.setRepeats(false);
            depTimer.start();
        } catch (Exception ex) {
            mensagemErro = ex.getMessage().toUpperCase();
            estadoAtual = EstadoTela.ERRO;
            atualizarTela();
        }
    }

    private void dispararImpressaoExtrato() {
        estadoAtual = EstadoTela.ANIMACAO_IMPRESSAO;
        atualizarTela();

        lblPrinterStatus.setText("IMPRIMINDO...");
        lblPrinterStatus.setForeground(Color.YELLOW);
        receiptPrinterContainer.setBackground(new Color(80, 80, 20));

        timerAnimacaoImpressao.start();
    }

    private void exibirComprovanteVirtual() {
        Conta conta = servicoCaixaEletronico.getContaAutenticada();
        if (conta == null)
            return;

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("               FIAP BANK                \n");
        sb.append("        COMPROVANTE DE EXTRATO          \n");
        sb.append("========================================\n");
        sb.append("CONTA: ").append(conta.getNumeroConta()).append("\n");
        sb.append("DATA: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("\n");
        sb.append("----------------------------------------\n");

        List<Transacao> txs = conta.getTransacoes();
        int count = 0;
        for (int i = txs.size() - 1; i >= 0 && count < 5; i--) {
            Transacao tx = txs.get(i);
            sb.append(String.format("%-12s %-14s %12s\n",
                    tx.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                    tx.getTipo().getDescricao(),
                    tx.getValor().formatar()));
            count++;
        }

        sb.append("----------------------------------------\n");
        sb.append("SALDO ATUAL: ").append(conta.getSaldo().formatar()).append("\n");
        sb.append("========================================\n");
        sb.append("        OBRIGADO POR UTILIZAR           \n");
        sb.append("             FIAP BANK                  \n");
        sb.append("========================================\n");

        if (dialogoRecibo != null) {
            dialogoRecibo.dispose();
        }

        dialogoRecibo = new JDialog(this, "Extrato Impresso", false);
        dialogoRecibo.setSize(320, 450);
        dialogoRecibo.setResizable(false);

        txtPapelRecibo = new JTextArea();
        txtPapelRecibo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtPapelRecibo.setBackground(new Color(250, 250, 245));
        txtPapelRecibo.setForeground(Color.BLACK);
        txtPapelRecibo.setText(sb.toString());
        txtPapelRecibo.setEditable(false);
        txtPapelRecibo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnTearOff = new JButton("Destacar Comprovante");
        btnTearOff.addActionListener(e -> dialogoRecibo.dispose());

        dialogoRecibo.setLayout(new BorderLayout());
        dialogoRecibo.add(new JScrollPane(txtPapelRecibo), BorderLayout.CENTER);
        dialogoRecibo.add(btnTearOff, BorderLayout.SOUTH);

        Point atmPos = this.getLocation();
        dialogoRecibo.setLocation(atmPos.x + this.getWidth() + 10, atmPos.y + 100);
        dialogoRecibo.setVisible(true);
    }

    private void atualizarTela() {
        lblLeftOpt1.setText(" ");
        lblLeftOpt2.setText(" ");
        lblLeftOpt3.setText(" ");
        lblRightOpt1.setText(" ");
        lblRightOpt2.setText(" ");
        lblRightOpt3.setText(" ");
        lblScreenMessage.setText(" ");

        if (servicoCaixaEletronico.estaAutenticado()) {
            lblCardIndicatorLed.setForeground(new Color(80, 80, 250));
            lblCardIndicatorLed.setText("● CARTÃO VALIDADO");
        } else if (estadoAtual == EstadoTela.BEM_VINDO) {
            // Controlado pelo timer (piscando verde)
        } else if (estadoAtual == EstadoTela.DIGITAR_SENHA) {
            lblCardIndicatorLed.setForeground(Color.ORANGE);
            lblCardIndicatorLed.setText("● LENDO SENHA...");
        } else if (estadoAtual == EstadoTela.ERRO) {
            lblCardIndicatorLed.setForeground(Color.RED);
            lblCardIndicatorLed.setText("● ERRO NO CARTÃO");
        }

        switch (estadoAtual) {
            case BEM_VINDO:
                lblScreenHeader.setText("--- ATM FIAP BANK ---");
                lblScreenStatus.setText("DIGITE O NÚMERO DA CONTA");
                lblScreenInput.setText(bufferEntrada.length() > 0 ? bufferEntrada.toString() + "_" : "[CONTA]_");
                lblScreenMessage.setText("Use o teclado físico ou numérico abaixo e clique Confirmar.");
                btnBlank.setText("Confirmar");
                break;

            case DIGITAR_SENHA:
                lblScreenHeader.setText("--- ATM FIAP BANK ---");
                lblScreenStatus.setText("INSIRA A SENHA DE 4 DÍGITOS");

                StringBuilder stars = new StringBuilder();
                for (int i = 0; i < bufferEntrada.length(); i++) {
                    stars.append("*");
                }
                lblScreenInput.setText(stars.length() > 0 ? stars.toString() : "[SENHA]");
                lblScreenMessage.setText("Acesso de Segurança. Pressione 'Confirmar' ao finalizar.");
                btnBlank.setText("Confirmar");
                break;

            case MENU_PRINCIPAL:
                Conta contaAutenticada = servicoCaixaEletronico.getContaAutenticada();
                lblScreenHeader.setText("--- MENU PRINCIPAL ---");
                lblScreenStatus
                        .setText("CONTA ATIVA: " + (contaAutenticada != null ? contaAutenticada.getNumeroConta() : ""));
                lblScreenInput.setText("SELECIONE A OPERAÇÃO");

                lblLeftOpt1.setText("> SACAR");
                lblLeftOpt2.setText("> DEPOSITAR");
                lblLeftOpt3.setText("> TRANSFERIR");

                lblRightOpt1.setText("SALDO <");
                lblRightOpt2.setText("EXTRATO <");
                lblRightOpt3.setText("SAIR <");
                btnBlank.setText("");
                break;

            case SELECIONAR_SAQUE:
                lblScreenHeader.setText("--- REALIZAR SAQUE ---");
                lblScreenStatus.setText("ESCOLHA O VALOR DO SAQUE");
                lblScreenInput.setText("");

                lblLeftOpt1.setText("> R$ 20,00");
                lblLeftOpt2.setText("> R$ 50,00");
                lblLeftOpt3.setText("> R$ 100,00");

                lblRightOpt1.setText("R$ 200,00 <");
                lblRightOpt2.setText("R$ 500,00 <");
                lblRightOpt3.setText("OUTRO VALOR <");
                btnBlank.setText("");
                lblScreenMessage.setText("Pressione 'C' no teclado para voltar ao Menu.");
                break;

            case SAQUE_PERSONALIZADO:
                lblScreenHeader.setText("--- VALOR PERSONALIZADO ---");
                lblScreenStatus.setText("DIGITE O VALOR PARA SAQUE");
                lblScreenInput
                        .setText(bufferEntrada.length() > 0 ? "R$ " + bufferEntrada.toString() + ",00" : "R$ 0,00");
                lblScreenMessage.setText("Pressione 'Confirmar' para realizar o saque.");
                btnBlank.setText("Confirmar");
                break;

            case DIGITAR_DEPOSITO:
                lblScreenHeader.setText("--- REALIZAR DEPÓSITO ---");
                lblScreenStatus.setText("INSIRA O VALOR DE DEPÓSITO");
                lblScreenInput
                        .setText(bufferEntrada.length() > 0 ? "R$ " + bufferEntrada.toString() + ",00" : "R$ 0,00");
                lblScreenMessage.setText("Digite e clique em 'Confirmar' para validar.");
                btnBlank.setText("Confirmar");
                break;

            case TRANSFERENCIA_CONTA:
                lblScreenHeader.setText("--- TRANSFERÊNCIA BANCÁRIA ---");
                lblScreenStatus.setText("DIGITE A CONTA DE DESTINO");
                lblScreenInput
                        .setText(bufferEntrada.length() > 0 ? bufferEntrada.toString() + "_" : "[CONTA DESTINO]_");
                lblScreenMessage.setText("Digite o número e clique 'Confirmar'.");
                btnBlank.setText("Confirmar");
                break;

            case TRANSFERENCIA_VALOR:
                lblScreenHeader.setText("--- VALOR DA TRANSFERÊNCIA ---");
                lblScreenStatus.setText("DESTINO: CONTA " + numeroContaDestino);
                lblScreenInput
                        .setText(bufferEntrada.length() > 0 ? "R$ " + bufferEntrada.toString() + ",00" : "R$ 0,00");
                lblScreenMessage.setText("Pressione 'Confirmar' para efetuar.");
                btnBlank.setText("Confirmar");
                break;

            case EXIBIR_SALDO:
                Conta contaSaldo = servicoCaixaEletronico.getContaAutenticada();
                lblScreenHeader.setText("--- CONSULTA DE SALDO ---");
                lblScreenStatus.setText("SALDO DISPONÍVEL");
                lblScreenInput.setText(contaSaldo != null ? contaSaldo.getSaldo().formatar() : "R$ 0,00");
                lblScreenMessage.setText("Limite Diário Restante: " +
                        (contaSaldo != null
                                ? contaSaldo.getLimiteSaqueDiario().menos(contaSaldo.getTotalSacadoHoje()).formatar()
                                : "R$ 0,00"));
                lblRightOpt3.setText("VOLTAR <");
                btnBlank.setText("");
                break;

            case EXIBIR_EXTRATO:
                lblScreenHeader.setText("--- EXTRATO IMPRESSO ---");
                lblScreenStatus.setText("EXTRATO GERADO");
                lblScreenInput.setText("VERIFIQUE A LATERAL");
                lblScreenMessage.setText("O extrato físico foi impresso na impressora lateral.");
                lblRightOpt3.setText("VOLTAR <");
                btnBlank.setText("");
                break;

            case ANIMACAO_SAQUE:
                lblScreenHeader.setText("--- AGUARDE ---");
                lblScreenStatus.setText("CONSTANDO CÉDULAS...");
                lblScreenInput.setText("$$$$$$$$$$$$$");
                lblScreenMessage.setText("Retire as cédulas no dispensador abaixo.");
                btnBlank.setText("");
                break;

            case ANIMACAO_IMPRESSAO:
                lblScreenHeader.setText("--- AGUARDE ---");
                lblScreenStatus.setText("IMPRIMINDO COMPROVANTE...");
                lblScreenInput.setText("■■■■■■■■■■■■■");
                lblScreenMessage.setText("Retire o papel térmico impresso ao lado.");
                btnBlank.setText("");
                break;

            case ANIMACAO_DEPOSITO:
                lblScreenHeader.setText("--- AGUARDE ---");
                lblScreenStatus.setText("PROCESSANDO DEPÓSITO...");
                lblScreenInput.setText("•••••••••••••");
                lblScreenMessage.setText("Processando envelopes e autenticação...");
                btnBlank.setText("");
                break;

            case SUCESSO:
                lblScreenHeader.setText("--- OPERAÇÃO CONCLUÍDA ---");
                lblScreenStatus.setText("TRANSAÇÃO COM SUCESSO!");
                lblScreenInput.setText("OBRIGADO");
                lblScreenMessage.setText("Retornando em instantes...");
                btnBlank.setText("");
                break;

            case ERRO:
                lblScreenHeader.setText("--- ATENÇÃO ---");
                lblScreenStatus.setText("FALHA NA OPERAÇÃO");
                lblScreenInput.setText("ERRO");
                lblScreenMessage.setText(mensagemErro != null ? mensagemErro : "TENTE NOVAMENTE.");
                btnBlank.setText("");
                break;
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanelMain = new javax.swing.JPanel();
        jPanelHeader = new javax.swing.JPanel();
        lblHeaderTitle = new javax.swing.JLabel();
        jPanelCenterConsole = new javax.swing.JPanel();
        jPanelLeftButtons = new javax.swing.JPanel();
        btnLeft1 = new javax.swing.JButton();
        btnLeft2 = new javax.swing.JButton();
        btnLeft3 = new javax.swing.JButton();
        jPanelRightButtons = new javax.swing.JPanel();
        btnRight1 = new javax.swing.JButton();
        btnRight2 = new javax.swing.JButton();
        btnRight3 = new javax.swing.JButton();
        jPanelScreen = new javax.swing.JPanel();
        jPanelScreenHeader = new javax.swing.JPanel();
        lblScreenHeader = new javax.swing.JLabel();
        jPanelScreenCenter = new javax.swing.JPanel();
        lblScreenStatus = new javax.swing.JLabel();
        lblScreenInput = new javax.swing.JLabel();
        lblScreenMessage = new javax.swing.JLabel();
        jPanelScreenLeftLabels = new javax.swing.JPanel();
        lblLeftOpt1 = new javax.swing.JLabel();
        lblLeftOpt2 = new javax.swing.JLabel();
        lblLeftOpt3 = new javax.swing.JLabel();
        jPanelScreenRightLabels = new javax.swing.JPanel();
        lblRightOpt1 = new javax.swing.JLabel();
        lblRightOpt2 = new javax.swing.JLabel();
        lblRightOpt3 = new javax.swing.JLabel();
        jPanelBottomConsole = new javax.swing.JPanel();
        jPanelKeypad = new javax.swing.JPanel();
        btn1 = new javax.swing.JButton();
        btn2 = new javax.swing.JButton();
        btn3 = new javax.swing.JButton();
        btn4 = new javax.swing.JButton();
        btn5 = new javax.swing.JButton();
        btn6 = new javax.swing.JButton();
        btn7 = new javax.swing.JButton();
        btn8 = new javax.swing.JButton();
        btn9 = new javax.swing.JButton();
        btnBlank = new javax.swing.JButton();
        btn0 = new javax.swing.JButton();
        btnC = new javax.swing.JButton();
        jPanelPeripherals = new javax.swing.JPanel();
        cardSlotContainer = new javax.swing.JPanel();
        lblCardIndicatorLed = new javax.swing.JLabel();
        receiptPrinterContainer = new javax.swing.JPanel();
        lblPrinterStatus = new javax.swing.JLabel();
        cashDispenserContainer = new javax.swing.JPanel();
        lblCashDispenserStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FIAP Bank - Caixa Eletrônico (ATM)");
        setResizable(false);

        jPanelMain.setBackground(new java.awt.Color(19, 30, 43));
        jPanelMain.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 51, 51), 8));
        jPanelMain.setLayout(new java.awt.BorderLayout());

        jPanelHeader.setBackground(new java.awt.Color(13, 20, 31));
        jPanelHeader.setPreferredSize(new java.awt.Dimension(800, 80));
        jPanelHeader.setLayout(new java.awt.GridBagLayout());

        lblHeaderTitle.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        lblHeaderTitle.setForeground(new java.awt.Color(241, 248, 252));
        lblHeaderTitle.setText("FIAP BANK");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = -1;
        gridBagConstraints.gridy = -1;
        jPanelHeader.add(lblHeaderTitle, gridBagConstraints);

        jPanelMain.add(jPanelHeader, java.awt.BorderLayout.NORTH);

        jPanelCenterConsole.setBackground(new java.awt.Color(19, 30, 43));
        jPanelCenterConsole.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 40, 20, 40));
        jPanelCenterConsole.setLayout(new java.awt.BorderLayout());

        jPanelLeftButtons.setBackground(new java.awt.Color(19, 30, 43));
        jPanelLeftButtons.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 0, 20, 15));
        jPanelLeftButtons.setPreferredSize(new java.awt.Dimension(100, 300));
        jPanelLeftButtons.setLayout(new java.awt.GridLayout(3, 1, 0, 35));

        btnLeft1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLeft1.setText("[ ]");
        jPanelLeftButtons.add(btnLeft1);

        btnLeft2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLeft2.setText("[ ]");
        jPanelLeftButtons.add(btnLeft2);

        btnLeft3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLeft3.setText("[ ]");
        jPanelLeftButtons.add(btnLeft3);

        jPanelCenterConsole.add(jPanelLeftButtons, java.awt.BorderLayout.WEST);

        jPanelRightButtons.setBackground(new java.awt.Color(19, 30, 43));
        jPanelRightButtons.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 15, 0, 0));
        jPanelRightButtons.setPreferredSize(new java.awt.Dimension(100, 300));
        jPanelRightButtons.setLayout(new java.awt.GridLayout(3, 1, 0, 35));

        btnRight1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRight1.setText("[ ]");
        jPanelRightButtons.add(btnRight1);

        btnRight2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRight2.setText("[ ]");
        jPanelRightButtons.add(btnRight2);

        btnRight3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRight3.setText("[ ]");
        jPanelRightButtons.add(btnRight3);

        jPanelCenterConsole.add(jPanelRightButtons, java.awt.BorderLayout.EAST);

        jPanelScreen.setBackground(new java.awt.Color(11, 18, 28));
        jPanelScreen.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(43, 56, 77), 4, true));
        jPanelScreen.setLayout(new java.awt.BorderLayout());

        jPanelScreenHeader.setBackground(new java.awt.Color(11, 18, 28));
        jPanelScreenHeader.setPreferredSize(new java.awt.Dimension(500, 45));

        lblScreenHeader.setFont(new java.awt.Font("Monospaced", 1, 18)); // NOI18N
        lblScreenHeader.setForeground(new java.awt.Color(254, 240, 138));
        lblScreenHeader.setText("--- ATM FIAP BANK ---");
        jPanelScreenHeader.add(lblScreenHeader);

        jPanelScreen.add(jPanelScreenHeader, java.awt.BorderLayout.NORTH);

        jPanelScreenCenter.setBackground(new java.awt.Color(11, 18, 28));
        jPanelScreenCenter.setLayout(new java.awt.GridLayout(3, 1));

        lblScreenStatus.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        lblScreenStatus.setForeground(new java.awt.Color(241, 245, 249));
        lblScreenStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblScreenStatus.setText("INSIRA SEU CARTÃO OU CONTA");
        jPanelScreenCenter.add(lblScreenStatus);

        lblScreenInput.setFont(new java.awt.Font("Monospaced", 1, 24)); // NOI18N
        lblScreenInput.setForeground(new java.awt.Color(56, 189, 248));
        lblScreenInput.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblScreenInput.setText("_");
        jPanelScreenCenter.add(lblScreenInput);

        lblScreenMessage.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblScreenMessage.setForeground(new java.awt.Color(234, 113, 113));
        lblScreenMessage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblScreenMessage.setText(" ");
        jPanelScreenCenter.add(lblScreenMessage);

        jPanelScreen.add(jPanelScreenCenter, java.awt.BorderLayout.CENTER);

        jPanelScreenLeftLabels.setBackground(new java.awt.Color(11, 18, 28));
        jPanelScreenLeftLabels.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 10, 20, 0));
        jPanelScreenLeftLabels.setPreferredSize(new java.awt.Dimension(130, 200));
        jPanelScreenLeftLabels.setLayout(new java.awt.GridLayout(3, 1, 0, 35));

        lblLeftOpt1.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        lblLeftOpt1.setForeground(new java.awt.Color(56, 189, 248));
        lblLeftOpt1.setText(" ");
        jPanelScreenLeftLabels.add(lblLeftOpt1);

        lblLeftOpt2.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        lblLeftOpt2.setForeground(new java.awt.Color(56, 189, 248));
        lblLeftOpt2.setText(" ");
        jPanelScreenLeftLabels.add(lblLeftOpt2);

        lblLeftOpt3.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        lblLeftOpt3.setForeground(new java.awt.Color(56, 189, 248));
        lblLeftOpt3.setText(" ");
        jPanelScreenLeftLabels.add(lblLeftOpt3);

        jPanelScreen.add(jPanelScreenLeftLabels, java.awt.BorderLayout.WEST);

        jPanelScreenRightLabels.setBackground(new java.awt.Color(11, 18, 28));
        jPanelScreenRightLabels.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 0, 20, 10));
        jPanelScreenRightLabels.setPreferredSize(new java.awt.Dimension(130, 200));
        jPanelScreenRightLabels.setLayout(new java.awt.GridLayout(3, 1, 0, 35));

        lblRightOpt1.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        lblRightOpt1.setForeground(new java.awt.Color(56, 189, 248));
        lblRightOpt1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblRightOpt1.setText(" ");
        jPanelScreenRightLabels.add(lblRightOpt1);

        lblRightOpt2.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        lblRightOpt2.setForeground(new java.awt.Color(56, 189, 248));
        lblRightOpt2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblRightOpt2.setText(" ");
        jPanelScreenRightLabels.add(lblRightOpt2);

        lblRightOpt3.setFont(new java.awt.Font("Monospaced", 1, 14)); // NOI18N
        lblRightOpt3.setForeground(new java.awt.Color(56, 189, 248));
        lblRightOpt3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblRightOpt3.setText(" ");
        jPanelScreenRightLabels.add(lblRightOpt3);

        jPanelScreen.add(jPanelScreenRightLabels, java.awt.BorderLayout.EAST);

        jPanelCenterConsole.add(jPanelScreen, java.awt.BorderLayout.CENTER);

        jPanelMain.add(jPanelCenterConsole, java.awt.BorderLayout.CENTER);

        jPanelBottomConsole.setBackground(new java.awt.Color(13, 20, 31));
        jPanelBottomConsole.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(85, 85, 85), 2), "CONSOLE DO OPERADOR",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Segoe UI", 1, 12), new java.awt.Color(170, 170, 170))); // NOI18N
        jPanelBottomConsole.setPreferredSize(new java.awt.Dimension(800, 360));
        jPanelBottomConsole.setLayout(new java.awt.GridLayout(1, 2, 30, 0));

        jPanelKeypad.setBackground(new java.awt.Color(13, 20, 31));
        jPanelKeypad.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 30, 15, 15));
        jPanelKeypad.setLayout(new java.awt.GridLayout(4, 3, 10, 10));

        btn1.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn1.setText("1");
        jPanelKeypad.add(btn1);

        btn2.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn2.setText("2");
        jPanelKeypad.add(btn2);

        btn3.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn3.setText("3");
        jPanelKeypad.add(btn3);

        btn4.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn4.setText("4");
        jPanelKeypad.add(btn4);

        btn5.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn5.setText("5");
        jPanelKeypad.add(btn5);

        btn6.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn6.setText("6");
        jPanelKeypad.add(btn6);

        btn7.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn7.setText("7");
        jPanelKeypad.add(btn7);

        btn8.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn8.setText("8");
        jPanelKeypad.add(btn8);

        btn9.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn9.setText("9");
        jPanelKeypad.add(btn9);

        btnBlank.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        btnBlank.setText("Cartão");
        jPanelKeypad.add(btnBlank);

        btn0.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btn0.setText("0");
        jPanelKeypad.add(btn0);

        btnC.setBackground(new java.awt.Color(189, 58, 58));
        btnC.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        btnC.setForeground(new java.awt.Color(255, 255, 255));
        btnC.setText("C");
        jPanelKeypad.add(btnC);

        jPanelBottomConsole.add(jPanelKeypad);

        jPanelPeripherals.setBackground(new java.awt.Color(13, 20, 31));
        jPanelPeripherals.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 30, 30));
        jPanelPeripherals.setLayout(new java.awt.GridLayout(3, 1, 0, 15));

        cardSlotContainer.setBackground(new java.awt.Color(18, 27, 38));
        cardSlotContainer.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(38, 52, 71), 2), "ENTRADA DE CARTÃO",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Segoe UI", 1, 10), new java.awt.Color(153, 153, 153))); // NOI18N
        cardSlotContainer.setLayout(new java.awt.BorderLayout());

        lblCardIndicatorLed.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblCardIndicatorLed.setForeground(new java.awt.Color(80, 200, 80));
        lblCardIndicatorLed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCardIndicatorLed.setText("● AGUARDANDO CARTÃO");
        cardSlotContainer.add(lblCardIndicatorLed, java.awt.BorderLayout.CENTER);

        jPanelPeripherals.add(cardSlotContainer);

        receiptPrinterContainer.setBackground(new java.awt.Color(18, 27, 38));
        receiptPrinterContainer.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(38, 52, 71), 2), "IMPRESSORA DE EXTRATO",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Segoe UI", 1, 10), new java.awt.Color(153, 153, 153))); // NOI18N
        receiptPrinterContainer.setLayout(new java.awt.BorderLayout());

        lblPrinterStatus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPrinterStatus.setForeground(new java.awt.Color(204, 204, 204));
        lblPrinterStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPrinterStatus.setText("PRONTA");
        receiptPrinterContainer.add(lblPrinterStatus, java.awt.BorderLayout.CENTER);

        jPanelPeripherals.add(receiptPrinterContainer);

        cashDispenserContainer.setBackground(new java.awt.Color(18, 27, 38));
        cashDispenserContainer.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(38, 52, 71), 2), "DISPENSADOR DE CÉDULAS",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Segoe UI", 1, 10), new java.awt.Color(153, 153, 153))); // NOI18N
        cashDispenserContainer.setLayout(new java.awt.BorderLayout());

        lblCashDispenserStatus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblCashDispenserStatus.setForeground(new java.awt.Color(153, 153, 153));
        lblCashDispenserStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCashDispenserStatus.setText("FECHADO");
        cashDispenserContainer.add(lblCashDispenserStatus, java.awt.BorderLayout.CENTER);

        jPanelPeripherals.add(cashDispenserContainer);

        jPanelBottomConsole.add(jPanelPeripherals);

        jPanelMain.add(jPanelBottomConsole, java.awt.BorderLayout.SOUTH);

        getContentPane().add(jPanelMain, java.awt.BorderLayout.CENTER);

        setSize(new java.awt.Dimension(816, 839));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn0;
    private javax.swing.JButton btn1;
    private javax.swing.JButton btn2;
    private javax.swing.JButton btn3;
    private javax.swing.JButton btn4;
    private javax.swing.JButton btn5;
    private javax.swing.JButton btn6;
    private javax.swing.JButton btn7;
    private javax.swing.JButton btn8;
    private javax.swing.JButton btn9;
    private javax.swing.JButton btnBlank;
    private javax.swing.JButton btnC;
    private javax.swing.JButton btnLeft1;
    private javax.swing.JButton btnLeft2;
    private javax.swing.JButton btnLeft3;
    private javax.swing.JButton btnRight1;
    private javax.swing.JButton btnRight2;
    private javax.swing.JButton btnRight3;
    private javax.swing.JPanel cardSlotContainer;
    private javax.swing.JPanel cashDispenserContainer;
    private javax.swing.JPanel jPanelBottomConsole;
    private javax.swing.JPanel jPanelCenterConsole;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelKeypad;
    private javax.swing.JPanel jPanelLeftButtons;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelPeripherals;
    private javax.swing.JPanel jPanelRightButtons;
    private javax.swing.JPanel jPanelScreen;
    private javax.swing.JPanel jPanelScreenCenter;
    private javax.swing.JPanel jPanelScreenHeader;
    private javax.swing.JPanel jPanelScreenLeftLabels;
    private javax.swing.JPanel jPanelScreenRightLabels;
    private javax.swing.JLabel lblCardIndicatorLed;
    private javax.swing.JLabel lblCashDispenserStatus;
    private javax.swing.JLabel lblHeaderTitle;
    private javax.swing.JLabel lblLeftOpt1;
    private javax.swing.JLabel lblLeftOpt2;
    private javax.swing.JLabel lblLeftOpt3;
    private javax.swing.JLabel lblPrinterStatus;
    private javax.swing.JLabel lblRightOpt1;
    private javax.swing.JLabel lblRightOpt2;
    private javax.swing.JLabel lblRightOpt3;
    private javax.swing.JLabel lblScreenHeader;
    private javax.swing.JLabel lblScreenInput;
    private javax.swing.JLabel lblScreenMessage;
    private javax.swing.JLabel lblScreenStatus;
    private javax.swing.JPanel receiptPrinterContainer;
    // End of variables declaration//GEN-END:variables
}
