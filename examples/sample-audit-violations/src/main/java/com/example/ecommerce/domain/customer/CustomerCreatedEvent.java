package com.example.ecommerce.domain.customer;

import com.example.ecommerce.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new customer registers on the e-commerce platform.
 *
 * <p>This event captures the initial registration data including the customer's
 * identity, full name, and email address. It is emitted by the {@link Customer#create}
 * factory method and can be consumed by downstream services to trigger welcome
 * emails, initialize loyalty accounts, or synchronize with CRM systems.
 *
 * <p>This event follows proper DDD naming conventions using the past tense.
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
