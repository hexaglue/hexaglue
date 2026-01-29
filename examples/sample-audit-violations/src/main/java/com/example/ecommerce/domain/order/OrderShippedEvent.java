package com.example.ecommerce.domain.order;

import com.example.ecommerce.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an order is dispatched to the carrier for delivery.
 *
 * <p>This event captures the shipping tracking number and carrier name, enabling
 * downstream consumers to send shipment notifications to customers, update
 * tracking dashboards, and trigger delivery monitoring workflows.
 *
 * <p>This event follows proper DDD naming conventions using the past tense.
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
