package com.example.enterprise.warehouse.domain.exception;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate2Id;

/**
 * Exception thrown when a WarehouseAggregate2 is not found.
 */
public class WarehouseAggregate2NotFoundException extends WarehouseDomainException {
    public WarehouseAggregate2NotFoundException(WarehouseAggregate2Id id) {
        super("WarehouseAggregate2 not found with id: " + id.value());
    }
}
