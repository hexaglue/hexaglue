package com.example.enterprise.shipping.port.driving;

import java.util.List;

/**
 * Command to create a new ShippingAggregate2.
 */
public record CreateShippingAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateShippingAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
