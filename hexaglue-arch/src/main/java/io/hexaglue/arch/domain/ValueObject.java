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
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Objects;

/**
 * A Value Object in Domain-Driven Design.
 *
 * <p>Value objects are immutable and defined by their attributes rather than identity.
 * They are compared by value equality, not reference equality.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Immutable - once created, cannot be changed</li>
 *   <li>No identity - defined by their attributes</li>
 *   <li>Side-effect free - operations return new instances</li>
 *   <li>Often implemented as Java records</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In domain model:
 * public record Money(BigDecimal amount, Currency currency) {}
 *
 * // As ArchElement:
 * ValueObject vo = new ValueObject(
 *     ElementId.of("com.example.Money"),
 *     List.of("amount", "currency"),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param componentFields the names of fields that define the value
 * @param syntax the syntax information from source analysis (nullable for synthetic types)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record ValueObject(
        ElementId id, List<String> componentFields, TypeSyntax syntax, ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new ValueObject instance.
     *
     * @param id the identifier, must not be null
     * @param componentFields the component fields, must not be null
     * @param syntax the syntax (can be null for synthetic types)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if id, componentFields, or classificationTrace is null
     */
    public ValueObject {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(componentFields, "componentFields must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        componentFields = List.copyOf(componentFields);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.VALUE_OBJECT;
    }

    /**
     * Returns whether this value object is a record type.
     *
     * @return true if implemented as a Java record
     */
    public boolean isRecord() {
        return syntax != null && syntax.form() == io.hexaglue.syntax.TypeForm.RECORD;
    }

    /**
     * Creates a simple ValueObject for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param fields the component field names
     * @param trace the classification trace
     * @return a new ValueObject
     */
    public static ValueObject of(String qualifiedName, List<String> fields, ClassificationTrace trace) {
        return new ValueObject(ElementId.of(qualifiedName), fields, null, trace);
    }
}
