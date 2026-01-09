package com.example.enterprise.analytics.domain.model;

/**
 * Value Object representing priority levels in analytics context.
 */
public enum AnalyticsPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    AnalyticsPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(AnalyticsPriority other) {
        return this.level > other.level;
    }
}
