package com.example.enterprise.catalog.domain.model;

/**
 * Value Object representing priority levels in catalog context.
 */
public enum CatalogPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    CatalogPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(CatalogPriority other) {
        return this.level > other.level;
    }
}
