package com.example.enterprise.shipping.port.driving;

import com.example.enterprise.shipping.domain.model.ShippingAggregate2Id;

/**
 * Command to update an existing ShippingAggregate2.
 */
public record UpdateShippingAggregate2Command(
    ShippingAggregate2Id id,
    String name
) {
    public UpdateShippingAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
