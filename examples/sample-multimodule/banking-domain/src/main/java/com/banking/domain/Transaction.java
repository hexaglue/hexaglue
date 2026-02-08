package com.banking.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Financial transaction within an account.
 *
 * @since 5.0.0
 */
public class Transaction {

    private final String id;
    private final long amount;
    private final TransactionType type;
    private final Instant timestamp;
    private final String description;

    public Transaction(String id, long amount, TransactionType type, String description) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.timestamp = Instant.now();
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }
}
