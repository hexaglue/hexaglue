package com.example.enterprise.supplier.domain.model;

import java.util.UUID;

public record SupplierItem2Id(UUID value) {
    public SupplierItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static SupplierItem2Id generate() {
        return new SupplierItem2Id(UUID.randomUUID());
    }
}
