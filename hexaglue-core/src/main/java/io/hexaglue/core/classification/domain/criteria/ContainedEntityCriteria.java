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
 * Matches types that have identity and are contained within aggregate-like types.
 *
 * <p>This is the complement of {@link EmbeddedValueObjectCriteria}: while value objects
 * are embedded types WITHOUT identity, entities are embedded types WITH identity.
 *
 * <p>Detection conditions:
 * <ul>
 *   <li>Type is a class (not record, enum, or interface)</li>
 *   <li>Type HAS an identity field ({@code id} or {@code <typeName>Id})</li>
 *   <li>Type is used as a field or collection element in an aggregate-like container</li>
 * </ul>
 *
 * <p>Example: {@code OrderLine} has a {@code Long id} field and is used in
 * {@code Order}'s {@code List<OrderLine>} collection — it is an entity within
 * the Order aggregate.
 *
 * <p>Priority: 70 (same as EmbeddedValueObjectCriteria — structural heuristic)
 * <p>Confidence: HIGH
 *
 * @since 5.0.0
 */
public final class ContainedEntityCriteria implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    @Override
    public String id() {
        return "domain.structural.containedEntity";
    }

    @Override
    public String name() {
        return "contained-entity";
    }

    @Override
    public int priority() {
        return 70;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.ENTITY;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a class (entities are mutable, not records/enums/interfaces)
        if (node.form() != JavaForm.CLASS) {
            return MatchResult.noMatch();
        }

        // Must HAVE an identity field (distinguishes from value objects)
        if (!hasIdentityField(node, query)) {
            return MatchResult.noMatch();
        }

        // Find types that have this type as a direct field
        List<TypeNode> fieldTypeContainers = query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.FIELD_TYPE)
                .map(Edge::from)
                .map(fieldId -> query.graph().indexes().declaringTypeOf(fieldId))
                .flatMap(Optional::stream)
                .map(query::type)
                .flatMap(Optional::stream)
                .toList();

        // Find types that use this type as a collection element
        List<TypeNode> collectionContainers = query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.USES_AS_COLLECTION_ELEMENT)
                .map(Edge::from)
                .map(query::type)
                .flatMap(Optional::stream)
                .toList();

        // Combine both sources
        List<TypeNode> containers = java.util.stream.Stream.concat(
                        fieldTypeContainers.stream(), collectionContainers.stream())
                .distinct()
                .toList();

        if (containers.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Filter to aggregate-like containers (types with identity fields)
        List<TypeNode> aggregateContainers =
                containers.stream().filter(c -> hasIdentityField(c, query)).toList();

        if (aggregateContainers.isEmpty()) {
            return MatchResult.noMatch();
        }

        String containerNames =
                aggregateContainers.stream().map(TypeNode::simpleName).collect(joining(", "));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Class with identity contained in aggregate(s): %s".formatted(containerNames),
                List.of(
                        Evidence.fromRelationship(
                                "Contained in: " + containerNames,
                                aggregateContainers.stream().map(TypeNode::id).toList()),
                        Evidence.fromStructure("Has identity field", List.of(node.id()))));
    }

    private boolean hasIdentityField(TypeNode type, GraphQuery query) {
        return query.fieldsOf(type).stream().anyMatch(field -> isIdentityField(type, field));
    }

    /**
     * Checks if a field represents the identity of the containing type.
     *
     * <p>Uses the same convention as {@link EmbeddedValueObjectCriteria}:
     * {@code id} or {@code <typeName>Id}.
     */
    private boolean isIdentityField(TypeNode type, FieldNode field) {
        String name = field.simpleName();

        if (name.equals("id")) {
            return true;
        }

        String typeName = type.simpleName();
        String expectedIdFieldName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1) + "Id";
        return name.equals(expectedIdFieldName);
    }

    @Override
    public String description() {
        return "Classifies classes with identity contained in aggregate-like types as ENTITY";
    }
}
