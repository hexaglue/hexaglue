package com.example.enterprise.shipping.domain.exception;

import com.example.enterprise.shipping.domain.model.ShippingAggregate3Id;

/**
 * Exception thrown when a ShippingAggregate3 is not found.
 */
public class ShippingAggregate3NotFoundException extends ShippingDomainException {
    public ShippingAggregate3NotFoundException(ShippingAggregate3Id id) {
        super("ShippingAggregate3 not found with id: " + id.value());
    }
}
