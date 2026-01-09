package com.example.enterprise.marketing.domain.model;

import java.util.UUID;

public record MarketingItem1Id(UUID value) {
    public MarketingItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static MarketingItem1Id generate() {
        return new MarketingItem1Id(UUID.randomUUID());
    }
}
