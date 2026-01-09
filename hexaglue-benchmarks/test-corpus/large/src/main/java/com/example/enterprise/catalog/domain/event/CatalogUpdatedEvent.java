package com.example.enterprise.catalog.domain.event;

import com.example.enterprise.catalog.domain.model.CatalogAggregate1Id;

/**
 * Event fired when a CatalogAggregate1 is updated.
 */
public class CatalogUpdatedEvent extends CatalogEvent {
    private final CatalogAggregate1Id aggregateId;

    public CatalogUpdatedEvent(CatalogAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public CatalogAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "CatalogUpdated";
    }
}
