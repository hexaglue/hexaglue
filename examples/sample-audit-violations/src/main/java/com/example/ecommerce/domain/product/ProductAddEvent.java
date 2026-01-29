package com.example.ecommerce.domain.product;

import com.example.ecommerce.domain.shared.DomainEvent;
import com.example.ecommerce.domain.order.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new product is added to the e-commerce catalog.
 *
 * <p>This event captures the product identity, display name, and initial price.
 * It is consumed by search indexers, catalog synchronization processes, and
 * marketing systems to feature newly available products.
 *
 * <p>AUDIT VIOLATION: ddd:event-naming.
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
