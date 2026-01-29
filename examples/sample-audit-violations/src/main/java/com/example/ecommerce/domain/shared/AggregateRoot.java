package com.example.ecommerce.domain.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for all aggregate roots in the e-commerce domain.
 *
 * <p>Provides domain event collection capabilities. Aggregate roots can register
 * events during state transitions, which are later harvested by the application
 * layer for publication to event listeners, message brokers, or event stores.
 *
 * <p>The event list should be cleared after events have been dispatched to
 * prevent duplicate processing.
 */
public abstract class AggregateRoot<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public abstract ID getId();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
