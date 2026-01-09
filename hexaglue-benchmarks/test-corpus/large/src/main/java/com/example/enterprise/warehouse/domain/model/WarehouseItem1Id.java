package com.example.enterprise.warehouse.domain.model;

import java.util.UUID;

public record WarehouseItem1Id(UUID value) {
    public WarehouseItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static WarehouseItem1Id generate() {
        return new WarehouseItem1Id(UUID.randomUUID());
    }
}
