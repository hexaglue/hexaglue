package com.example.enterprise.marketing.domain.exception;

import com.example.enterprise.marketing.domain.model.MarketingAggregate3Id;

/**
 * Exception thrown when a MarketingAggregate3 is not found.
 */
public class MarketingAggregate3NotFoundException extends MarketingDomainException {
    public MarketingAggregate3NotFoundException(MarketingAggregate3Id id) {
        super("MarketingAggregate3 not found with id: " + id.value());
    }
}
