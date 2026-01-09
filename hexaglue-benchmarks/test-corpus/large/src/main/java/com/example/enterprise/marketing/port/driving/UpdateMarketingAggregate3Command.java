package com.example.enterprise.marketing.port.driving;

import com.example.enterprise.marketing.domain.model.MarketingAggregate3Id;

/**
 * Command to update an existing MarketingAggregate3.
 */
public record UpdateMarketingAggregate3Command(
    MarketingAggregate3Id id,
    String name
) {
    public UpdateMarketingAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
