package com.example.enterprise.warehouse.domain.exception;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate3Id;

/**
 * Exception thrown when a WarehouseAggregate3 is not found.
 */
public class WarehouseAggregate3NotFoundException extends WarehouseDomainException {
    public WarehouseAggregate3NotFoundException(WarehouseAggregate3Id id) {
        super("WarehouseAggregate3 not found with id: " + id.value());
    }
}
