package com.regression.domain.order;

import java.util.UUID;

/**
 * Identifier for the Order aggregate.
 * <p>
 * Tests C1/C4: Repository methods like getById(OrderId), loadById(OrderId),
 * fetchById(OrderId) should all be implemented correctly.
 */
public record OrderId(UUID value) {

    public OrderId {
        if (value == null) {
            throw new IllegalArgumentException("OrderId value cannot be null");
        }
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
