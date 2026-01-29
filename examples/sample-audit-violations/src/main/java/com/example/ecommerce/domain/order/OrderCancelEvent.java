package com.example.ecommerce.domain.order;

import com.example.ecommerce.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an order is cancelled by the customer or the system.
 *
 * <p>This event captures the cancellation reason, enabling downstream systems to
 * release reserved inventory, issue refunds, and update analytics dashboards.
 *
 * <p>AUDIT VIOLATION: ddd:event-naming.
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
