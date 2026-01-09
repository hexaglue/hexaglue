package com.example.enterprise.marketing.domain.exception;

import com.example.enterprise.marketing.domain.model.MarketingAggregate2Id;

/**
 * Exception thrown when a MarketingAggregate2 is not found.
 */
public class MarketingAggregate2NotFoundException extends MarketingDomainException {
    public MarketingAggregate2NotFoundException(MarketingAggregate2Id id) {
        super("MarketingAggregate2 not found with id: " + id.value());
    }
}
