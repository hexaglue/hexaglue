package com.example.validation.domain;

import java.util.UUID;

/**
 * Order identifier value object.
 *
 * <p>Classification: EXPLICIT via hexaglue.yaml configuration.
 *
 * <p>This class has no jMolecules annotation, but is explicitly classified
 * as VALUE_OBJECT in hexaglue.yaml:
 *
 * <pre>
 * classification:
 *   explicit:
 *     com.example.validation.domain.OrderId: VALUE_OBJECT
 * </pre>
 *
 * <p>This demonstrates how to classify types without modifying source code,
 * useful for:
 * <ul>
 *   <li>Third-party classes you cannot annotate</li>
 *   <li>Legacy code where adding dependencies is not desired</li>
 *   <li>Quick overrides during migration</li>
 * </ul>
 */
public record OrderId(UUID value) {

    public OrderId {
        if (value == null) {
            throw new IllegalArgumentException("OrderId value cannot be null");
        }
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
