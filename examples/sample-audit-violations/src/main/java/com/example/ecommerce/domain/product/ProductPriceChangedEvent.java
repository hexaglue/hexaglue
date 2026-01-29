package com.example.ecommerce.domain.product;

import com.example.ecommerce.domain.shared.DomainEvent;
import com.example.ecommerce.domain.order.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a product's price is modified in the catalog.
 *
 * <p>This event captures both the previous and new price, enabling price history
 * tracking, customer price alert notifications, and competitive pricing analytics.
 * It is emitted by {@link Product#changePrice} whenever the price is updated.
 *
 * <p>This event follows proper DDD naming conventions using the past tense.
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
