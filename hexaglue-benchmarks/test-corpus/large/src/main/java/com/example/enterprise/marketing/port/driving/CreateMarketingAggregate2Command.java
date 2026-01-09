package com.example.enterprise.marketing.port.driving;

import java.util.List;

/**
 * Command to create a new MarketingAggregate2.
 */
public record CreateMarketingAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateMarketingAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
