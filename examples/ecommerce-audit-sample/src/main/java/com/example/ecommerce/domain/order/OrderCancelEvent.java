package com.example.ecommerce.domain.order;

import com.example.ecommerce.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when an order is cancelled.
 *
 * AUDIT VIOLATION: ddd:event-naming
 * This event should be named "OrderCancelledEvent" (past tense).
 */
public record OrderCancelEvent(
        UUID eventId,
        Instant occurredAt,
        OrderId orderId,
        String reason
) implements DomainEvent {

    public OrderCancelEvent(OrderId orderId, String reason) {
        this(UUID.randomUUID(), Instant.now(), orderId, reason);
    }
}
