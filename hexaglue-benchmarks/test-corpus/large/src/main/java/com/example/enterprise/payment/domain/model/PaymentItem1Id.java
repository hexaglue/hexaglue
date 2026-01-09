package com.example.enterprise.payment.domain.model;

import java.util.UUID;

public record PaymentItem1Id(UUID value) {
    public PaymentItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static PaymentItem1Id generate() {
        return new PaymentItem1Id(UUID.randomUUID());
    }
}
