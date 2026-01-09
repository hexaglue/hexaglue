package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.WarehouseId;

/**
 * Exception thrown when Warehouse is not found.
 */
public class WarehouseNotFoundException extends DomainException {
    private final WarehouseId id;

    public WarehouseNotFoundException(WarehouseId id) {
        super("Warehouse not found: " + id);
        this.id = id;
    }

    public WarehouseId getId() {
        return id;
    }
}
