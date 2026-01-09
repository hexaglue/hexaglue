package com.example.enterprise.catalog.domain.event;

import com.example.enterprise.catalog.domain.model.CatalogAggregate1Id;

/**
 * Event fired when a CatalogAggregate1 is created.
 */
public class CatalogCreatedEvent extends CatalogEvent {
    private final CatalogAggregate1Id aggregateId;
    private final String name;

    public CatalogCreatedEvent(CatalogAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public CatalogAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "CatalogCreated";
    }
}
