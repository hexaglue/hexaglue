package com.example.enterprise.customer.domain.model;

/**
 * Value Object representing priority levels in customer context.
 */
public enum CustomerPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    CustomerPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(CustomerPriority other) {
        return this.level > other.level;
    }
}
