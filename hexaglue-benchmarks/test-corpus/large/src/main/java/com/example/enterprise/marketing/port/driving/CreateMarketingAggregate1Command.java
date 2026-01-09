package com.example.enterprise.marketing.port.driving;

import java.util.List;

/**
 * Command to create a new MarketingAggregate1.
 */
public record CreateMarketingAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateMarketingAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
