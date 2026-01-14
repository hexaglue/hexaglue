package com.example.ecommerce.domain.order;

import com.example.ecommerce.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when an order is shipped.
 * This event follows proper naming conventions (past tense).
 */
public record OrderShippedEvent(
        UUID eventId,
        Instant occurredAt,
        OrderId orderId,
        String trackingNumber,
        String carrier
) implements DomainEvent {

    public OrderShippedEvent(OrderId orderId, String trackingNumber, String carrier) {
        this(UUID.randomUUID(), Instant.now(), orderId, trackingNumber, carrier);
    }
}
