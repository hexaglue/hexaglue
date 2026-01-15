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
import io.hexaglue.arch.ElementRef;
import io.hexaglue.syntax.TypeRef;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A Domain Entity in Domain-Driven Design.
 *
 * <p>Entities are objects with a distinct identity that persists over time.
 * Unlike value objects, two entities can have identical attributes but remain distinct.</p>
 *
 * <p>This type represents both:</p>
 * <ul>
 *   <li>{@link ElementKind#ENTITY} - Internal entities within an aggregate</li>
 *   <li>{@link ElementKind#AGGREGATE_ROOT} - The root entity of an aggregate (entry point)</li>
 * </ul>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Has a unique identity (usually an ID field)</li>
 *   <li>Mutable - state can change over time</li>
 *   <li>Has lifecycle - created, modified, potentially deleted</li>
 *   <li>Compared by identity, not attributes</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // In domain model:
 * public class OrderLine {
 *     private OrderLineId id;
 *     private ProductId productId;
 *     private int quantity;
 * }
 *
 * // As ArchElement:
 * DomainEntity entity = new DomainEntity(
 *     ElementId.of("com.example.OrderLine"),
 *     ElementKind.ENTITY,
 *     "id",
 *     TypeRef.of("com.example.OrderLineId"),
 *     Optional.of(aggregateRef),
 *     List.of(productIdRef),
 *     typeSyntax,
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier
 * @param entityKind the specific kind (ENTITY or AGGREGATE_ROOT)
 * @param identityField the name of the identity field (if found)
 * @param identityType the type of the identity field (if found)
 * @param owningAggregate reference to the aggregate this entity belongs to (if known)
 * @param valueObjectRefs references to value objects used by this entity
 * @param syntax the syntax information from source analysis (nullable for synthetic types)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record DomainEntity(
        ElementId id,
        ElementKind entityKind,
        String identityField,
        TypeRef identityType,
        Optional<ElementRef<Aggregate>> owningAggregate,
        List<ElementRef<ValueObject>> valueObjectRefs,
        TypeSyntax syntax,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new DomainEntity instance.
     *
     * @param id the identifier, must not be null
     * @param entityKind must be ENTITY or AGGREGATE_ROOT
     * @param identityField the identity field name (can be null)
     * @param identityType the identity type (can be null)
     * @param owningAggregate reference to owning aggregate, must not be null
     * @param valueObjectRefs value object references, must not be null
     * @param syntax the syntax (can be null for synthetic types)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if required fields are null
     * @throws IllegalArgumentException if entityKind is not ENTITY or AGGREGATE_ROOT
     */
    public DomainEntity {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(entityKind, "entityKind must not be null");
        Objects.requireNonNull(owningAggregate, "owningAggregate must not be null (use Optional.empty())");
        Objects.requireNonNull(valueObjectRefs, "valueObjectRefs must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        if (entityKind != ElementKind.ENTITY && entityKind != ElementKind.AGGREGATE_ROOT) {
            throw new IllegalArgumentException("entityKind must be ENTITY or AGGREGATE_ROOT, got: " + entityKind);
        }
        valueObjectRefs = List.copyOf(valueObjectRefs);
    }

    @Override
    public ElementKind kind() {
        return entityKind;
    }

    /**
     * Returns whether this entity is an aggregate root.
     *
     * @return true if this is an aggregate root
     */
    public boolean isAggregateRoot() {
        return entityKind == ElementKind.AGGREGATE_ROOT;
    }

    /**
     * Returns whether this entity has an identity field.
     *
     * @return true if identity field is known
     */
    public boolean hasIdentity() {
        return identityField != null && !identityField.isBlank();
    }

    /**
     * Creates a simple entity for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new DomainEntity
     */
    public static DomainEntity entity(String qualifiedName, ClassificationTrace trace) {
        return new DomainEntity(
                ElementId.of(qualifiedName), ElementKind.ENTITY, null, null, Optional.empty(), List.of(), null, trace);
    }

    /**
     * Creates a simple aggregate root for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new DomainEntity with AGGREGATE_ROOT kind
     */
    public static DomainEntity aggregateRoot(String qualifiedName, ClassificationTrace trace) {
        return new DomainEntity(
                ElementId.of(qualifiedName),
                ElementKind.AGGREGATE_ROOT,
                null,
                null,
                Optional.empty(),
                List.of(),
                null,
                trace);
    }
}
