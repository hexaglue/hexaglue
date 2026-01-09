package com.example.enterprise.payment.domain.model;

/**
 * Value Object representing priority levels in payment context.
 */
public enum PaymentPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    PaymentPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(PaymentPriority other) {
        return this.level > other.level;
    }
}
