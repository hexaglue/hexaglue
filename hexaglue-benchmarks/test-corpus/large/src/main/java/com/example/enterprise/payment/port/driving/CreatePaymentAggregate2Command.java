package com.example.enterprise.payment.port.driving;

import java.util.List;

/**
 * Command to create a new PaymentAggregate2.
 */
public record CreatePaymentAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreatePaymentAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
