package com.example.enterprise.payment.domain.model;

import java.util.UUID;

public record PaymentItem2Id(UUID value) {
    public PaymentItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static PaymentItem2Id generate() {
        return new PaymentItem2Id(UUID.randomUUID());
    }
}
