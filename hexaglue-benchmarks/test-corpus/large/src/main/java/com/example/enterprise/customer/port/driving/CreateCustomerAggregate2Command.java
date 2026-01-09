package com.example.enterprise.customer.port.driving;

import java.util.List;

/**
 * Command to create a new CustomerAggregate2.
 */
public record CreateCustomerAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateCustomerAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
