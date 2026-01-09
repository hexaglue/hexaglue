package com.example.enterprise.payment.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for payment domain events.
 */
public abstract class PaymentEvent {
    private final UUID eventId;
    private final Instant occurredAt;

    protected PaymentEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
