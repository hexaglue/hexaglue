package com.example.enterprise.inventory.domain.exception;

import com.example.enterprise.inventory.domain.model.InventoryAggregate1Id;

/**
 * Exception thrown when a InventoryAggregate1 is not found.
 */
public class InventoryAggregate1NotFoundException extends InventoryDomainException {
    public InventoryAggregate1NotFoundException(InventoryAggregate1Id id) {
        super("InventoryAggregate1 not found with id: " + id.value());
    }
}
