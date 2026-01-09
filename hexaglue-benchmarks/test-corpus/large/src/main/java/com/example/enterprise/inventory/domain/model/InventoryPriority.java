package com.example.enterprise.inventory.domain.model;

/**
 * Value Object representing priority levels in inventory context.
 */
public enum InventoryPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    InventoryPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(InventoryPriority other) {
        return this.level > other.level;
    }
}
