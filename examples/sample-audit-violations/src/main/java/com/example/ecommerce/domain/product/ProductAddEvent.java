package com.example.ecommerce.domain.product;

import com.example.ecommerce.domain.shared.DomainEvent;
import com.example.ecommerce.domain.order.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a product is added to the catalog.
 *
 * AUDIT VIOLATION: ddd:event-naming
 * This event should be named "ProductAddedEvent" (past tense).
 * "ProductAddEvent" sounds like a command, not an event.
 */
public record ProductAddEvent(
        UUID eventId,
        Instant occurredAt,
        ProductId productId,
        String name,
        Money price
) implements DomainEvent {

    public ProductAddEvent(ProductId productId, String name, Money price) {
        this(UUID.randomUUID(), Instant.now(), productId, name, price);
    }
}
