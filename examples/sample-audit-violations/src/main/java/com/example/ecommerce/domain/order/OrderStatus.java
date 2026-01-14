package com.example.ecommerce.domain.order;

/**
 * Enumeration of possible order statuses.
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
