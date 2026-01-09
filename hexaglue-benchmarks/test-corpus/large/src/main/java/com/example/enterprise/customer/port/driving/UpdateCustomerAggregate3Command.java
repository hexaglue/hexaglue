package com.example.enterprise.customer.port.driving;

import com.example.enterprise.customer.domain.model.CustomerAggregate3Id;

/**
 * Command to update an existing CustomerAggregate3.
 */
public record UpdateCustomerAggregate3Command(
    CustomerAggregate3Id id,
    String name
) {
    public UpdateCustomerAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
