package com.example.enterprise.customer.domain.model;

import java.util.UUID;

public record CustomerItem3Id(UUID value) {
    public CustomerItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static CustomerItem3Id generate() {
        return new CustomerItem3Id(UUID.randomUUID());
    }
}
