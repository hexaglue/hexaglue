package com.example.ecommerce.domain.customer;

import com.example.ecommerce.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a customer's default billing address is updated.
 *
 * <p>This event captures both the previous and new address, enabling audit trails
 * and downstream notifications (e.g., fraud detection, shipping recalculation).
 * It is emitted by {@link Customer#setDefaultBillingAddress} when the billing
 * address changes.
 *
 * <p>This event follows proper DDD naming conventions using the past tense.
 */
public record CustomerAddressChangedEvent(
        UUID eventId,
        Instant occurredAt,
        CustomerId customerId,
        Address oldAddress,
        Address newAddress
) implements DomainEvent {

    public CustomerAddressChangedEvent(CustomerId customerId, Address oldAddress, Address newAddress) {
        this(UUID.randomUUID(), Instant.now(), customerId, oldAddress, newAddress);
    }
}
