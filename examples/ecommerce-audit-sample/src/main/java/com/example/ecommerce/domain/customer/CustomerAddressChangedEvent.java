package com.example.ecommerce.domain.customer;

import com.example.ecommerce.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a customer's address is changed.
 * This event follows proper naming conventions (past tense).
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
