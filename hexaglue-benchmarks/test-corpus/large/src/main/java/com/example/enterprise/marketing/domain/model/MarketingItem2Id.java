package com.example.enterprise.marketing.domain.model;

import java.util.UUID;

public record MarketingItem2Id(UUID value) {
    public MarketingItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static MarketingItem2Id generate() {
        return new MarketingItem2Id(UUID.randomUUID());
    }
}
