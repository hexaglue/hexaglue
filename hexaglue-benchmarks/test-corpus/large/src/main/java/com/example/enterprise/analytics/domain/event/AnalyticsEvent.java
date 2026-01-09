package com.example.enterprise.analytics.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for analytics domain events.
 */
public abstract class AnalyticsEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected AnalyticsEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
