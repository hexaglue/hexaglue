package com.example.enterprise.marketing.domain.model;

/**
 * Value Object representing priority levels in marketing context.
 */
public enum MarketingPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    MarketingPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(MarketingPriority other) {
        return this.level > other.level;
    }
}
