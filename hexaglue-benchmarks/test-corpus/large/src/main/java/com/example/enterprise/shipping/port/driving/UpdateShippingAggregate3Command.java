package com.example.enterprise.shipping.port.driving;

import com.example.enterprise.shipping.domain.model.ShippingAggregate3Id;

/**
 * Command to update an existing ShippingAggregate3.
 */
public record UpdateShippingAggregate3Command(
    ShippingAggregate3Id id,
    String name
) {
    public UpdateShippingAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
