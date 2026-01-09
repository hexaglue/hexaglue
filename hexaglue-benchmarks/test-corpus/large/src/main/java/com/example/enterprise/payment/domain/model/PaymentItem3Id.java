package com.example.enterprise.payment.domain.model;

import java.util.UUID;

public record PaymentItem3Id(UUID value) {
    public PaymentItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static PaymentItem3Id generate() {
        return new PaymentItem3Id(UUID.randomUUID());
    }
}
