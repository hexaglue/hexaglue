package com.example.enterprise.warehouse.domain.exception;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1Id;

/**
 * Exception thrown when a WarehouseAggregate1 is not found.
 */
public class WarehouseAggregate1NotFoundException extends WarehouseDomainException {
    public WarehouseAggregate1NotFoundException(WarehouseAggregate1Id id) {
        super("WarehouseAggregate1 not found with id: " + id.value());
    }
}
