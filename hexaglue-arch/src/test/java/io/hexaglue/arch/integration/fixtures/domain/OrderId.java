package io.hexaglue.arch.integration.fixtures.domain;

import java.util.UUID;

/**
 * Identifier for Order aggregate.
 */
@Identifier
public record OrderId(UUID value) {

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }
}
