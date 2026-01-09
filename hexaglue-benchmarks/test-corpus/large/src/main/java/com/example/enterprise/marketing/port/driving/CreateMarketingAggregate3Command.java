package com.example.enterprise.marketing.port.driving;

import java.util.List;

/**
 * Command to create a new MarketingAggregate3.
 */
public record CreateMarketingAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateMarketingAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
