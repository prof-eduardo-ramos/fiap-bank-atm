package com.fiap.bank.atm.domain.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Transaction {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LocalDateTime timestamp;
    private final TransactionType type;
    private final Money amount;
    private final String description;

    public Transaction(TransactionType type, Money amount, String description) {
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    // For testing/mocking historical transactions
    public Transaction(LocalDateTime timestamp, TransactionType type, Money amount, String description) {
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (%s)", getFormattedTimestamp(), type.getDescription(), amount, description);
    }
}
