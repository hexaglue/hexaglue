package com.example.ecommerce.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in the e-commerce bounded contexts.
 *
 * <p>Every domain event carries a unique identifier and the instant at which it
 * occurred. Events represent significant state changes within aggregate roots
 * and are used for inter-context communication, audit logging, and event-driven
 * integrations.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();
}
