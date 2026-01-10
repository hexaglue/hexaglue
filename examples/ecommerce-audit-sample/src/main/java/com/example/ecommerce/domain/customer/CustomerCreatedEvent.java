package com.example.ecommerce.domain.customer;

import com.example.ecommerce.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a new customer is created.
 * This event follows proper naming conventions (past tense).
 */
public record CustomerCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        CustomerId customerId,
        String firstName,
        String lastName,
        Email email
) implements DomainEvent {

    public CustomerCreatedEvent(CustomerId customerId, String firstName, String lastName, Email email) {
        this(UUID.randomUUID(), Instant.now(), customerId, firstName, lastName, email);
    }
}
