package com.example.enterprise.payment.port.driving;

import java.util.List;

/**
 * Command to create a new PaymentAggregate3.
 */
public record CreatePaymentAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreatePaymentAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
