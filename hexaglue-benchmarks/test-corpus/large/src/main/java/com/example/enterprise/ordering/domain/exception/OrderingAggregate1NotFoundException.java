package com.example.enterprise.ordering.domain.exception;

import com.example.enterprise.ordering.domain.model.OrderingAggregate1Id;

/**
 * Exception thrown when a OrderingAggregate1 is not found.
 */
public class OrderingAggregate1NotFoundException extends OrderingDomainException {
    public OrderingAggregate1NotFoundException(OrderingAggregate1Id id) {
        super("OrderingAggregate1 not found with id: " + id.value());
    }
}
