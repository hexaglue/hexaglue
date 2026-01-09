package com.example.enterprise.inventory.domain.exception;

import com.example.enterprise.inventory.domain.model.InventoryAggregate3Id;

/**
 * Exception thrown when a InventoryAggregate3 is not found.
 */
public class InventoryAggregate3NotFoundException extends InventoryDomainException {
    public InventoryAggregate3NotFoundException(InventoryAggregate3Id id) {
        super("InventoryAggregate3 not found with id: " + id.value());
    }
}
