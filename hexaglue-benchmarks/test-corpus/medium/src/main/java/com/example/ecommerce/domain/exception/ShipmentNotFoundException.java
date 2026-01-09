package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.ShipmentId;

/**
 * Exception thrown when Shipment is not found.
 */
public class ShipmentNotFoundException extends DomainException {
    private final ShipmentId id;

    public ShipmentNotFoundException(ShipmentId id) {
        super("Shipment not found: " + id);
        this.id = id;
    }

    public ShipmentId getId() {
        return id;
    }
}
