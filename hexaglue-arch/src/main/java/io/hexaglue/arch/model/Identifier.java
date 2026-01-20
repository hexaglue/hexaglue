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
import io.hexaglue.syntax.TypeRef;
import java.util.Objects;

/**
 * Represents an identifier type in the domain model.
 *
 * <p>An identifier is a value object that represents the identity of an entity
 * or aggregate root. Identifiers typically wrap a primitive type (like UUID,
 * Long, or String) and provide type safety and domain expressiveness.</p>
 *
 * <h2>Wrapped Type</h2>
 * <p>The {@link #wrappedType()} returns the underlying primitive type that this
 * identifier wraps. For example, an {@code OrderId} might wrap a {@code UUID}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Identifier orderId = Identifier.of(
 *     TypeId.of("com.example.OrderId"),
 *     structure,
 *     trace,
 *     TypeRef.of("java.util.UUID")
 * );
 *
 * // Access the wrapped type
 * TypeRef wrapped = orderId.wrappedType(); // UUID
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @param wrappedType the underlying type that this identifier wraps
 * @since 4.1.0
 */
public record Identifier(TypeId id, TypeStructure structure, ClassificationTrace classification, TypeRef wrappedType)
        implements DomainType {

    /**
     * Creates a new Identifier.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @param wrappedType the wrapped type, must not be null
     * @throws NullPointerException if any argument is null
     */
    public Identifier {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(wrappedType, "wrappedType must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.IDENTIFIER;
    }

    /**
     * Creates an Identifier with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param wrappedType the wrapped type (e.g., UUID, Long, String)
     * @return a new Identifier
     * @throws NullPointerException if any argument is null
     */
    public static Identifier of(
            TypeId id, TypeStructure structure, ClassificationTrace classification, TypeRef wrappedType) {
        return new Identifier(id, structure, classification, wrappedType);
    }
}
