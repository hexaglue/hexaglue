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

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.syntax.TypeRef;
import io.hexaglue.syntax.TypeSyntax;
import java.util.Objects;

/**
 * An Identifier type in Domain-Driven Design.
 *
 * <p>Identifiers are value objects that uniquely identify entities and aggregates.
 * They provide type safety and domain meaning to what would otherwise be primitive IDs.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Wraps an underlying primitive or value type</li>
 *   <li>Usually immutable</li>
 *   <li>Named after the entity they identify (e.g., OrderId, CustomerId)</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In domain model:
 * public record OrderId(UUID value) {}
 *
 * // As ArchElement:
 * Identifier id = new Identifier(
 *     ElementId.of("com.example.OrderId"),
 *     TypeRef.of("java.util.UUID"),
 *     "com.example.Order",
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier of this type
 * @param wrappedType the underlying type being wrapped (e.g., UUID, Long, String)
 * @param identifiesType the qualified name of the type this identifier identifies (if known)
 * @param syntax the syntax information from source analysis (nullable for synthetic types)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record Identifier(
        ElementId id,
        TypeRef wrappedType,
        String identifiesType,
        TypeSyntax syntax,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new Identifier instance.
     *
     * @param id the identifier, must not be null
     * @param wrappedType the wrapped type (can be null if unknown)
     * @param identifiesType the type this identifies (can be null if unknown)
     * @param syntax the syntax (can be null for synthetic types)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if id or classificationTrace is null
     */
    public Identifier {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
    }

    @Override
    public ElementKind kind() {
        return ElementKind.IDENTIFIER;
    }

    /**
     * Returns whether the wrapped type is known.
     *
     * @return true if the wrapped type is known
     */
    public boolean hasWrappedType() {
        return wrappedType != null;
    }

    /**
     * Returns whether this identifier identifies a known type.
     *
     * @return true if the identified type is known
     */
    public boolean hasIdentifiesType() {
        return identifiesType != null && !identifiesType.isBlank();
    }

    /**
     * Creates a simple Identifier for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new Identifier
     */
    public static Identifier of(String qualifiedName, ClassificationTrace trace) {
        return new Identifier(ElementId.of(qualifiedName), null, null, null, trace);
    }
}
