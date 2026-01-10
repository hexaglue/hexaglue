package com.example.ecommerce.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();
}
