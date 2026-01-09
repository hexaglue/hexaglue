package com.example.enterprise.marketing.domain.model;

import java.util.UUID;

public record MarketingItem3Id(UUID value) {
    public MarketingItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static MarketingItem3Id generate() {
        return new MarketingItem3Id(UUID.randomUUID());
    }
}
