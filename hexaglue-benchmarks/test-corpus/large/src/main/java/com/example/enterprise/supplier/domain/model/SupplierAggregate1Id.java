package com.example.enterprise.supplier.domain.model;

import java.util.UUID;

/**
 * Value Object representing a SupplierAggregate1 identifier.
 */
public record SupplierAggregate1Id(UUID value) {
    public SupplierAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("SupplierAggregate1Id cannot be null");
        }
    }

    public static SupplierAggregate1Id generate() {
        return new SupplierAggregate1Id(UUID.randomUUID());
    }

    public static SupplierAggregate1Id of(String value) {
        return new SupplierAggregate1Id(UUID.fromString(value));
    }
}
