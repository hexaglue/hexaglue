package io.hexaglue.arch.integration.fixtures.domain;

import java.time.Instant;

/**
 * Domain event raised when an order is placed.
 */
@DomainEvent
public record OrderPlaced(OrderId orderId, CustomerId customerId, Money total, Instant occurredAt) {

    public static OrderPlaced of(OrderId orderId, CustomerId customerId, Money total) {
        return new OrderPlaced(orderId, customerId, total, Instant.now());
    }
}
