package com.example.enterprise.shipping.domain.exception;

import com.example.enterprise.shipping.domain.model.ShippingAggregate2Id;

/**
 * Exception thrown when a ShippingAggregate2 is not found.
 */
public class ShippingAggregate2NotFoundException extends ShippingDomainException {
    public ShippingAggregate2NotFoundException(ShippingAggregate2Id id) {
        super("ShippingAggregate2 not found with id: " + id.value());
    }
}
