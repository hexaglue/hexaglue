package com.example.enterprise.shipping.domain.exception;

import com.example.enterprise.shipping.domain.model.ShippingAggregate1Id;

/**
 * Exception thrown when a ShippingAggregate1 is not found.
 */
public class ShippingAggregate1NotFoundException extends ShippingDomainException {
    public ShippingAggregate1NotFoundException(ShippingAggregate1Id id) {
        super("ShippingAggregate1 not found with id: " + id.value());
    }
}
