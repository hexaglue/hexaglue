package com.example.enterprise.shipping.port.driving;

import java.util.List;

/**
 * Command to create a new ShippingAggregate1.
 */
public record CreateShippingAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateShippingAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
