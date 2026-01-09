package com.example.enterprise.customer.domain.model;

import java.util.UUID;

public record CustomerItem1Id(UUID value) {
    public CustomerItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static CustomerItem1Id generate() {
        return new CustomerItem1Id(UUID.randomUUID());
    }
}
