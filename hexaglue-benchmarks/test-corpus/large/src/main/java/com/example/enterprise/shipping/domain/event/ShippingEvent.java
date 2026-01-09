package com.example.enterprise.shipping.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for shipping domain events.
 */
public abstract class ShippingEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected ShippingEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
