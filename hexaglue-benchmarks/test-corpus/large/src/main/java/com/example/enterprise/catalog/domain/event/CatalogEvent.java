package com.example.enterprise.catalog.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for catalog domain events.
 */
public abstract class CatalogEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected CatalogEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
