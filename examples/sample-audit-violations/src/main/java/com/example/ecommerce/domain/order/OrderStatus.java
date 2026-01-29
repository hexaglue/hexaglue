package com.example.ecommerce.domain.order;

/**
 * Enumeration representing the lifecycle states of an {@link Order}.
 *
 * <p>Defines the valid state transitions for the order state machine:
 * DRAFT may transition to PENDING_PAYMENT or CANCELLED;
 * PENDING_PAYMENT to PAID or CANCELLED; PAID to PROCESSING or REFUNDED;
 * PROCESSING to SHIPPED or CANCELLED; SHIPPED to DELIVERED.
 * Terminal states (DELIVERED, CANCELLED, REFUNDED) allow no further transitions.
 */
public enum OrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case DRAFT -> newStatus == PENDING_PAYMENT || newStatus == CANCELLED;
            case PENDING_PAYMENT -> newStatus == PAID || newStatus == CANCELLED;
            case PAID -> newStatus == PROCESSING || newStatus == REFUNDED;
            case PROCESSING -> newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED;
            case DELIVERED, CANCELLED, REFUNDED -> false;
        };
    }
}
