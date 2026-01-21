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

package io.hexaglue.arch.model;

import io.hexaglue.arch.ClassificationTrace;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a value object in the domain model.
 *
 * <p>A value object is an immutable type that is identified by its attributes
 * rather than by a unique identity. Value objects are used to describe aspects
 * of the domain with no conceptual identity, such as {@code Money}, {@code Address},
 * or {@code DateRange}.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>No identity - two value objects with the same attributes are equal</li>
 *   <li>Immutable - once created, the state cannot be changed</li>
 *   <li>Side-effect free - operations return new instances instead of modifying state</li>
 * </ul>
 *
 * <h2>Single-Value Detection (since 5.0.0)</h2>
 * <p>Value objects wrapping a single value (like {@code OrderId}, {@code CustomerId}) can be
 * detected using {@link #isSingleValue()} and the wrapped field accessed via {@link #wrappedField()}.
 * This is useful for code generation that needs to unwrap such value objects.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ValueObject money = ValueObject.of(
 *     TypeId.of("com.example.Money"),
 *     structure,
 *     trace
 * );
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @since 4.1.0
 */
public record ValueObject(TypeId id, TypeStructure structure, ClassificationTrace classification)
        implements DomainType {

    /**
     * Creates a new ValueObject.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @throws NullPointerException if any argument is null
     */
    public ValueObject {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.VALUE_OBJECT;
    }

    /**
     * Creates a ValueObject with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new ValueObject
     * @throws NullPointerException if any argument is null
     */
    public static ValueObject of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new ValueObject(id, structure, classification);
    }

    /**
     * Returns whether this value object wraps a single value.
     *
     * <p>A single-value value object has exactly one field. Such value objects
     * are often used as strongly-typed identifiers or simple wrappers around
     * primitive types (e.g., {@code OrderId} wrapping a {@code UUID}).</p>
     *
     * @return true if this value object has exactly one field
     * @since 5.0.0
     */
    public boolean isSingleValue() {
        return structure.fields().size() == 1;
    }

    /**
     * Returns the wrapped field if this is a single-value value object.
     *
     * <p>This method is useful for code generation that needs to unwrap
     * single-value value objects to their underlying type.</p>
     *
     * @return the wrapped field, or empty if not a single-value value object
     * @since 5.0.0
     */
    public Optional<Field> wrappedField() {
        return isSingleValue() ? Optional.of(structure.fields().get(0)) : Optional.empty();
    }
}
