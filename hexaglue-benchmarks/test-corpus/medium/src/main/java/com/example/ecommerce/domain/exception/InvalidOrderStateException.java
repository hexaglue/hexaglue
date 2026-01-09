package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.OrderStatus;

/**
 * Exception thrown when an order operation is invalid for the current state.
 */
public class InvalidOrderStateException extends DomainException {
    private final OrderStatus currentStatus;
    private final String operation;

    public InvalidOrderStateException(OrderStatus currentStatus, String operation) {
        super(String.format("Cannot %s order in status %s", operation, currentStatus));
        this.currentStatus = currentStatus;
        this.operation = operation;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getOperation() {
        return operation;
    }
}
