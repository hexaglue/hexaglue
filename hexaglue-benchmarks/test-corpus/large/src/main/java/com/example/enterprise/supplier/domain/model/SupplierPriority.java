package com.example.enterprise.supplier.domain.model;

/**
 * Value Object representing priority levels in supplier context.
 */
public enum SupplierPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    SupplierPriority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(SupplierPriority other) {
        return this.level > other.level;
    }
}
