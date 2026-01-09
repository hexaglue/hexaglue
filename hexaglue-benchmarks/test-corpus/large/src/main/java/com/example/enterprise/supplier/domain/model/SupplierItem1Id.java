package com.example.enterprise.supplier.domain.model;

import java.util.UUID;

public record SupplierItem1Id(UUID value) {
    public SupplierItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static SupplierItem1Id generate() {
        return new SupplierItem1Id(UUID.randomUUID());
    }
}
