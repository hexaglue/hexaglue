package com.example.enterprise.shipping.port.driving;

import java.util.List;

/**
 * Command to create a new ShippingAggregate3.
 */
public record CreateShippingAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateShippingAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
