package com.example.enterprise.supplier.domain.model;

import java.util.UUID;

/**
 * Value Object representing a SupplierAggregate2 identifier.
 */
public record SupplierAggregate2Id(UUID value) {
    public SupplierAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("SupplierAggregate2Id cannot be null");
        }
    }

    public static SupplierAggregate2Id generate() {
        return new SupplierAggregate2Id(UUID.randomUUID());
    }

    public static SupplierAggregate2Id of(String value) {
        return new SupplierAggregate2Id(UUID.fromString(value));
    }
}
