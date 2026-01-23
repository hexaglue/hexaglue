package com.regression.domain.order;

/**
 * Domain enum representing order status.
 * <p>
 * Tests H2: Enums in domain packages should be classified as VALUE_OBJECT.
 * This enables proper @Enumerated(EnumType.STRING) generation in JPA entities.
 */
public enum OrderStatus {
    DRAFT,
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
