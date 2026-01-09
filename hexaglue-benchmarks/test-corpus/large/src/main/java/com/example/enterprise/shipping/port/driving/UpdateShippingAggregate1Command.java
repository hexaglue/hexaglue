package com.example.enterprise.shipping.port.driving;

import com.example.enterprise.shipping.domain.model.ShippingAggregate1Id;

/**
 * Command to update an existing ShippingAggregate1.
 */
public record UpdateShippingAggregate1Command(
    ShippingAggregate1Id id,
    String name
) {
    public UpdateShippingAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
