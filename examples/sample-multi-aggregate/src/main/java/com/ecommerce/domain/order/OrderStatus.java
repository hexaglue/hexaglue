package com.ecommerce.domain.order;

/**
 * Order lifecycle status.
 */
public enum OrderStatus {
    DRAFT,
    PLACED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
