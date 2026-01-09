package com.example.enterprise.marketing.port.driving;

import com.example.enterprise.marketing.domain.model.MarketingAggregate2Id;

/**
 * Command to update an existing MarketingAggregate2.
 */
public record UpdateMarketingAggregate2Command(
    MarketingAggregate2Id id,
    String name
) {
    public UpdateMarketingAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
