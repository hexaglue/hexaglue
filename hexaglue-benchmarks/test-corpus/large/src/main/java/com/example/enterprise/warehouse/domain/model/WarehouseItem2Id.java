package com.example.enterprise.warehouse.domain.model;

import java.util.UUID;

public record WarehouseItem2Id(UUID value) {
    public WarehouseItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static WarehouseItem2Id generate() {
        return new WarehouseItem2Id(UUID.randomUUID());
    }
}
