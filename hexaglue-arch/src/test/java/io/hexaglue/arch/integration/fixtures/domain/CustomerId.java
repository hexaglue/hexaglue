package io.hexaglue.arch.integration.fixtures.domain;

import java.util.UUID;

/**
 * Identifier for Customer aggregate (external reference).
 */
@Identifier
public record CustomerId(UUID value) {

    public static CustomerId of(String value) {
        return new CustomerId(UUID.fromString(value));
    }
}
