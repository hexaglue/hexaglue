package com.example.enterprise.marketing.port.driving;

import com.example.enterprise.marketing.domain.model.MarketingAggregate1Id;

/**
 * Command to update an existing MarketingAggregate1.
 */
public record UpdateMarketingAggregate1Command(
    MarketingAggregate1Id id,
    String name
) {
    public UpdateMarketingAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
