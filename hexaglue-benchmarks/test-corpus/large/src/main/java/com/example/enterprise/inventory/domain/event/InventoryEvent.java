package com.example.enterprise.inventory.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for inventory domain events.
 */
public abstract class InventoryEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected InventoryEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
