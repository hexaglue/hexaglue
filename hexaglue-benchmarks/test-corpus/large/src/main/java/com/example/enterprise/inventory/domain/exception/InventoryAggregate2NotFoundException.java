package com.example.enterprise.inventory.domain.exception;

import com.example.enterprise.inventory.domain.model.InventoryAggregate2Id;

/**
 * Exception thrown when a InventoryAggregate2 is not found.
 */
public class InventoryAggregate2NotFoundException extends InventoryDomainException {
    public InventoryAggregate2NotFoundException(InventoryAggregate2Id id) {
        super("InventoryAggregate2 not found with id: " + id.value());
    }
}
