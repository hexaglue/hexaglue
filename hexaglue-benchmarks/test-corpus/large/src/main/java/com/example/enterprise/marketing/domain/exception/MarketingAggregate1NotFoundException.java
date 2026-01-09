package com.example.enterprise.marketing.domain.exception;

import com.example.enterprise.marketing.domain.model.MarketingAggregate1Id;

/**
 * Exception thrown when a MarketingAggregate1 is not found.
 */
public class MarketingAggregate1NotFoundException extends MarketingDomainException {
    public MarketingAggregate1NotFoundException(MarketingAggregate1Id id) {
        super("MarketingAggregate1 not found with id: " + id.value());
    }
}
