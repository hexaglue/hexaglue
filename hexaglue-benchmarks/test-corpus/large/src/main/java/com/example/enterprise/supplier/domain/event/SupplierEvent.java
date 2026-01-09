package com.example.enterprise.supplier.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for supplier domain events.
 */
public abstract class SupplierEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected SupplierEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
