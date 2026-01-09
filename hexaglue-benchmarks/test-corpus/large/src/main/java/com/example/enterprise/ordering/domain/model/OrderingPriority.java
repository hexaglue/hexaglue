package com.example.enterprise.ordering.domain.model;

/**
 * Value Object representing priority levels in ordering context.
 */
public enum OrderingPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    OrderingPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(OrderingPriority other) {
        return this.level > other.level;
    }
}
