package com.example.enterprise.ordering.domain.exception;

import com.example.enterprise.ordering.domain.model.OrderingAggregate3Id;

/**
 * Exception thrown when a OrderingAggregate3 is not found.
 */
public class OrderingAggregate3NotFoundException extends OrderingDomainException {
    public OrderingAggregate3NotFoundException(OrderingAggregate3Id id) {
        super("OrderingAggregate3 not found with id: " + id.value());
    }
}
