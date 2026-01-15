/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.arch.domain;

import io.hexaglue.arch.Cardinality;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.syntax.TypeRef;
import java.util.Objects;

/**
 * Reference to another aggregate via its identifier.
 *
 * <p>In DDD, aggregates should not hold direct object references to other aggregates.
 * Instead, they reference other aggregates by their ID. This record captures such
 * inter-aggregate references.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Order aggregate references Customer by CustomerId
 * public class Order {
 *     private CustomerId customerId;  // <- This is an AggregateReference
 * }
 *
 * AggregateReference ref = new AggregateReference(
 *     "customerId",
 *     ElementRef.of(customerAggId, Aggregate.class),
 *     TypeRef.of("com.example.CustomerId"),
 *     Cardinality.ONE
 * );
 * }</pre>
 *
 * @param propertyName the name of the field/property holding the reference
 * @param targetAggregate reference to the target aggregate
 * @param idType the type of the identifier used (e.g., CustomerId)
 * @param cardinality the cardinality of the reference
 * @since 4.0.0
 */
public record AggregateReference(
        String propertyName, ElementRef<Aggregate> targetAggregate, TypeRef idType, Cardinality cardinality) {

    /**
     * Creates a new AggregateReference instance.
     *
     * @param propertyName the property name, must not be null or blank
     * @param targetAggregate reference to target aggregate, must not be null
     * @param idType the ID type, must not be null
     * @param cardinality the cardinality, must not be null
     * @throws NullPointerException if any field is null
     * @throws IllegalArgumentException if propertyName is blank
     */
    public AggregateReference {
        Objects.requireNonNull(propertyName, "propertyName must not be null");
        Objects.requireNonNull(targetAggregate, "targetAggregate must not be null");
        Objects.requireNonNull(idType, "idType must not be null");
        Objects.requireNonNull(cardinality, "cardinality must not be null");
        if (propertyName.isBlank()) {
            throw new IllegalArgumentException("propertyName must not be blank");
        }
    }

    /**
     * Returns whether this is a required reference.
     *
     * @return true if at least one instance is required
     */
    public boolean isRequired() {
        return cardinality.isRequired();
    }

    /**
     * Returns whether this reference can have multiple targets.
     *
     * @return true if multiple targets are allowed
     */
    public boolean isMany() {
        return cardinality.isMany();
    }
}
