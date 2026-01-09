package com.example.enterprise.customer.port.driving;

import java.util.List;

/**
 * Command to create a new CustomerAggregate1.
 */
public record CreateCustomerAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateCustomerAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
