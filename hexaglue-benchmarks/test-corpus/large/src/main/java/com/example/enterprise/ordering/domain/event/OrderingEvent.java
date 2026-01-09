package com.example.enterprise.ordering.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for ordering domain events.
 */
public abstract class OrderingEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected OrderingEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
