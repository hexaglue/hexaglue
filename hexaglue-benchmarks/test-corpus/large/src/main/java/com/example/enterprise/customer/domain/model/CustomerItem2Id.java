package com.example.enterprise.customer.domain.model;

import java.util.UUID;

public record CustomerItem2Id(UUID value) {
    public CustomerItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static CustomerItem2Id generate() {
        return new CustomerItem2Id(UUID.randomUUID());
    }
}
