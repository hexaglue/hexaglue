package com.example.enterprise.customer.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for customer domain events.
 */
public abstract class CustomerEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected CustomerEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
