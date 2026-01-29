package com.example.ecommerce.domain.order;

import com.example.ecommerce.domain.shared.DomainEvent;
import com.example.ecommerce.domain.customer.CustomerId;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a customer confirms and submits an order for payment.
 *
 * <p>This event captures the order identity, the associated customer, and the total
 * monetary amount. It is consumed by payment processing workflows and inventory
 * reservation systems.
 *
 * <p>AUDIT VIOLATION: ddd:event-naming.
 * This event should be named "OrderPlacedEvent" (past tense).
 * "OrderPlaceEvent" sounds like a command, not an event.
 */
public record OrderPlaceEvent(
        UUID eventId,
        Instant occurredAt,
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount
) implements DomainEvent {

    public OrderPlaceEvent(OrderId orderId, CustomerId customerId, Money totalAmount) {
        this(UUID.randomUUID(), Instant.now(), orderId, customerId, totalAmount);
    }
}
