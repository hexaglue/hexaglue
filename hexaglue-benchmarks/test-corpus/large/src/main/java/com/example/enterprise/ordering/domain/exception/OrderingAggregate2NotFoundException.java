package com.example.enterprise.ordering.domain.exception;

import com.example.enterprise.ordering.domain.model.OrderingAggregate2Id;

/**
 * Exception thrown when a OrderingAggregate2 is not found.
 */
public class OrderingAggregate2NotFoundException extends OrderingDomainException {
    public OrderingAggregate2NotFoundException(OrderingAggregate2Id id) {
        super("OrderingAggregate2 not found with id: " + id.value());
    }
}
