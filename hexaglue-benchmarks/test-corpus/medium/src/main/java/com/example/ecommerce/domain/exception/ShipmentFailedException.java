package com.example.ecommerce.domain.exception;

/**
 * Exception thrown when Shipment failed.
 */
public class ShipmentFailedException extends DomainException {
    public ShipmentFailedException(String message) {
        super(message);
    }
}
