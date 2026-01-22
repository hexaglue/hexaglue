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

package io.hexaglue.core.classification.domain.criteria;

import static java.util.stream.Collectors.joining;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Optional;

/**
 * Matches types that are embedded in aggregate-like containers and have no identity.
 *
 * <p>This criteria detects Value Objects by analyzing their usage:
 * <ul>
 *   <li>Type is used as a field in another type (via FIELD_TYPE edge)</li>
 *   <li><b>OR</b> type is used as a collection element (via USES_AS_COLLECTION_ELEMENT edge)</li>
 *   <li>Container type has an identity field (aggregate-like) <b>OR</b> container is
 *       an immutable record without identity (itself a value object candidate)</li>
 *   <li>This type has NO identity field</li>
 *   <li>This type is immutable (record or all-final fields)</li>
 * </ul>
 *
 * <p>This is a graph-based heuristic that exploits the FIELD_TYPE and
 * USES_AS_COLLECTION_ELEMENT edges to detect Value Objects embedded in aggregates
 * or in other value objects, including those used in collections like {@code List<OrderLine>}.
 *
 * <p>Example: {@code Quantity} is used in {@code OrderLine}, and {@code OrderLine} is used
 * in {@code Order}. Both {@code Quantity} and {@code OrderLine} are classified as VALUE_OBJECT.
 *
 * <p>Priority: 70 (strong heuristic, below explicit annotations)
 * <p>Confidence: HIGH
 */
public final class EmbeddedValueObjectCriteria implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.structural.embeddedValueObject";
    }

    @Override
    public String name() {
        return "embedded-value-object";
    }

    @Override
    public int priority() {
        return 70;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.VALUE_OBJECT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a class or record, not an interface
        if (node.form() == JavaForm.INTERFACE || node.form() == JavaForm.ENUM) {
            return MatchResult.noMatch();
        }

        // Must NOT have an identity field (otherwise it's an entity)
        if (hasIdentityField(node, query)) {
            return MatchResult.noMatch();
        }

        // Find types that have this type as a field (via FIELD_TYPE edges pointing to this type)
        List<TypeNode> fieldTypeContainers = query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.FIELD_TYPE)
                .map(Edge::from)
                // The FIELD_TYPE edge comes from a FieldNode, so find its declaring type
                .map(fieldId -> query.graph().indexes().declaringTypeOf(fieldId))
                .flatMap(Optional::stream)
                .map(query::type)
                .flatMap(Optional::stream)
                .toList();

        // Also find types that use this type as a collection element
        // (e.g., List<OrderLine> in Order -> OrderLine is collection element)
        List<TypeNode> collectionContainers = query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.USES_AS_COLLECTION_ELEMENT)
                .map(Edge::from)
                .map(query::type)
                .flatMap(Optional::stream)
                .toList();

        // Combine both sources of containers
        List<TypeNode> containers = java.util.stream.Stream.concat(
                        fieldTypeContainers.stream(), collectionContainers.stream())
                .distinct()
                .toList();

        if (containers.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Filter to keep containers that are:
        // 1. Aggregate-like (types with identity), OR
        // 2. Value object candidates (immutable records without identity)
        List<TypeNode> eligibleContainers = containers.stream()
                .filter(c -> hasIdentityField(c, query) || isValueObjectCandidate(c, query))
                .toList();

        if (eligibleContainers.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Separate aggregate containers for reporting purposes
        List<TypeNode> aggregateContainers = eligibleContainers.stream()
                .filter(c -> hasIdentityField(c, query))
                .toList();

        // Check if this type is immutable (record or has only final fields)
        String containerNames =
                eligibleContainers.stream().map(TypeNode::simpleName).collect(joining(", "));

        // Determine confidence based on container type and immutability
        boolean hasAggregateContainer = !aggregateContainers.isEmpty();
        String containerType = hasAggregateContainer ? "aggregate(s)" : "value object(s)";

        if (!isImmutable(node, query)) {
            // Still match but with lower confidence
            return MatchResult.match(
                    ConfidenceLevel.MEDIUM,
                    "Embedded in %s [%s], no identity, but not immutable".formatted(containerType, containerNames),
                    List.of(
                            Evidence.fromRelationship(
                                    "Embedded in: " + containerNames,
                                    eligibleContainers.stream()
                                            .map(TypeNode::id)
                                            .toList()),
                            Evidence.fromStructure("No identity field", List.of(node.id()))));
        }

        // Higher confidence if embedded in aggregate, medium if only in value object candidates
        ConfidenceLevel confidence = hasAggregateContainer ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM;

        return MatchResult.match(
                confidence,
                "Immutable type embedded in %s: %s".formatted(containerType, containerNames),
                List.of(
                        Evidence.fromRelationship(
                                "Embedded in: " + containerNames,
                                eligibleContainers.stream().map(TypeNode::id).toList()),
                        Evidence.fromStructure("No identity, immutable", List.of(node.id()))));
    }

    private boolean hasIdentityField(TypeNode type, GraphQuery query) {
        return query.fieldsOf(type).stream().anyMatch(field -> isIdentityField(type, field));
    }

    /**
     * Checks if a field represents the identity of the containing type.
     *
     * <p>Identity fields are detected by name convention:
     * <ul>
     *   <li>{@code id} - generic identity field name</li>
     *   <li>{@code <typeName>Id} - type-specific identity (e.g., {@code orderId} for {@code Order})</li>
     * </ul>
     *
     * <p>Fields like {@code productId} in {@code OrderLine} are foreign key references,
     * not identity fields, because they don't match the containing type's name.
     */
    private boolean isIdentityField(TypeNode type, FieldNode field) {
        String name = field.simpleName();

        // Exact match on "id"
        if (name.equals("id")) {
            return true;
        }

        // Check for <typeName>Id pattern (e.g., orderId for Order, orderLineId for OrderLine)
        String typeName = type.simpleName();
        String expectedIdFieldName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1) + "Id";
        return name.equals(expectedIdFieldName);
    }

    private boolean isImmutable(TypeNode type, GraphQuery query) {
        // Records are immutable by definition
        if (type.isRecord()) {
            return true;
        }

        // For classes, check if all fields are final
        List<FieldNode> fields = query.fieldsOf(type);
        if (fields.isEmpty()) {
            return true; // No fields = immutable
        }

        return fields.stream().allMatch(FieldNode::isFinal);
    }

    /**
     * Checks if a type is a value object candidate (without identity).
     *
     * <p>A container is a value object candidate if:
     * <ul>
     *   <li>It has NO identity field</li>
     *   <li>It is NOT an ID wrapper (name ending with Id/ID)</li>
     *   <li>It is NOT an interface or enum</li>
     * </ul>
     *
     * <p>Note: We don't require immutability here because the container might be
     * a class with some mutable fields (like OrderLine with mutable quantity)
     * but still semantically a value object embedded in an aggregate.
     *
     * <p>This allows transitive detection of value objects: if Quantity is used
     * in OrderLine (itself embedded in Order), Quantity is also a value object.
     */
    private boolean isValueObjectCandidate(TypeNode type, GraphQuery query) {
        // Must be a class or record, not interface/enum
        if (type.form() == JavaForm.INTERFACE || type.form() == JavaForm.ENUM) {
            return false;
        }

        // Must NOT be an ID wrapper
        String simpleName = type.simpleName();
        if (simpleName.endsWith("Id") || simpleName.endsWith("ID")) {
            return false;
        }

        // Must NOT have identity
        return !hasIdentityField(type, query);
    }
}
