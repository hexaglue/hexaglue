package com.example.enterprise.warehouse.domain.model;

/**
 * Value Object representing priority levels in warehouse context.
 */
public enum WarehousePriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    WarehousePriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(WarehousePriority other) {
        return this.level > other.level;
    }
}
