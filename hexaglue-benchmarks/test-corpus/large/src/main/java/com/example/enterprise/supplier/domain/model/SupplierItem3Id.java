package com.example.enterprise.supplier.domain.model;

import java.util.UUID;

public record SupplierItem3Id(UUID value) {
    public SupplierItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static SupplierItem3Id generate() {
        return new SupplierItem3Id(UUID.randomUUID());
    }
}
