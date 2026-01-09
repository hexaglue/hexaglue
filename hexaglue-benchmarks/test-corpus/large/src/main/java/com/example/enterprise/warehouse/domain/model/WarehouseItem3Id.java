package com.example.enterprise.warehouse.domain.model;

import java.util.UUID;

public record WarehouseItem3Id(UUID value) {
    public WarehouseItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static WarehouseItem3Id generate() {
        return new WarehouseItem3Id(UUID.randomUUID());
    }
}
