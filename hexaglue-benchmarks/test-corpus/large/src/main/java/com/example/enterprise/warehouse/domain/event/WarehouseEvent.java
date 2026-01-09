package com.example.enterprise.warehouse.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for warehouse domain events.
 */
public abstract class WarehouseEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected WarehouseEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
