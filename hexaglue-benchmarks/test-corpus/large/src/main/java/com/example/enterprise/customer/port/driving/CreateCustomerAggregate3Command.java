package com.example.enterprise.customer.port.driving;

import java.util.List;

/**
 * Command to create a new CustomerAggregate3.
 */
public record CreateCustomerAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateCustomerAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
