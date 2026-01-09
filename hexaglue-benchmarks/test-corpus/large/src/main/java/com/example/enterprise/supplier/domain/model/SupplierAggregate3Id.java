package com.example.enterprise.supplier.domain.model;

import java.util.UUID;

/**
 * Value Object representing a SupplierAggregate3 identifier.
 */
public record SupplierAggregate3Id(UUID value) {
    public SupplierAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("SupplierAggregate3Id cannot be null");
        }
    }

    public static SupplierAggregate3Id generate() {
        return new SupplierAggregate3Id(UUID.randomUUID());
    }

    public static SupplierAggregate3Id of(String value) {
        return new SupplierAggregate3Id(UUID.fromString(value));
    }
}
