package com.banking.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Account aggregate root.
 *
 * @since 5.0.0
 */
public class Account {

    private final AccountId id;
    private final String ownerName;
    private long balance;
    private final Instant createdAt;
    private Address address;
    private final List<Transaction> transactions;

    public Account(AccountId id, String ownerName, long balance) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = Instant.now();
        this.transactions = new ArrayList<>();
    }

    public AccountId getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balance += amount;
        transactions.add(new Transaction(
                java.util.UUID.randomUUID().toString(), amount, TransactionType.DEPOSIT, "Deposit of " + amount));
    }

    public void withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (amount > this.balance) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance -= amount;
        transactions.add(new Transaction(
                java.util.UUID.randomUUID().toString(), amount, TransactionType.WITHDRAWAL, "Withdrawal of " + amount));
    }
}
