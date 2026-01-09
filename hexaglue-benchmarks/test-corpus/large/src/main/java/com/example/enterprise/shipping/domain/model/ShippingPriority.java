package com.example.enterprise.shipping.domain.model;

/**
 * Value Object representing priority levels in shipping context.
 */
public enum ShippingPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    ShippingPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(ShippingPriority other) {
        return this.level > other.level;
    }
}
