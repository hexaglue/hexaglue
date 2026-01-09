package com.example.enterprise.marketing.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for marketing domain events.
 */
public abstract class MarketingEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected MarketingEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
