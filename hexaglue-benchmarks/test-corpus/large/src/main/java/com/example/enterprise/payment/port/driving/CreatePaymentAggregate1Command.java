package com.example.enterprise.payment.port.driving;

import java.util.List;

/**
 * Command to create a new PaymentAggregate1.
 */
public record CreatePaymentAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreatePaymentAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
