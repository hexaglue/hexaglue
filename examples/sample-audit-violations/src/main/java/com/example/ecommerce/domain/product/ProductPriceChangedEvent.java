package com.example.ecommerce.domain.product;

import com.example.ecommerce.domain.shared.DomainEvent;
import com.example.ecommerce.domain.order.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a product's price changes.
 * This event follows proper naming conventions (past tense).
 */
public record ProductPriceChangedEvent(
        UUID eventId,
        Instant occurredAt,
        ProductId productId,
        Money oldPrice,
        Money newPrice
) implements DomainEvent {

    public ProductPriceChangedEvent(ProductId productId, Money oldPrice, Money newPrice) {
        this(UUID.randomUUID(), Instant.now(), productId, oldPrice, newPrice);
    }
}
