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

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
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
 *   <li>Container type has an identity field (aggregate-like)</li>
 *   <li>This type has NO identity field</li>
 *   <li>This type is immutable (record or all-final fields)</li>
 * </ul>
 *
 * <p>This is a graph-based heuristic that exploits the FIELD_TYPE edges.
 *
 * <p>Priority: 70 (strong heuristic, below explicit annotations)
 * <p>Confidence: HIGH
 */
public final class EmbeddedValueObjectCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "embedded-value-object";
    }

    @Override
    public int priority() {
        return 70;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.VALUE_OBJECT;
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
        List<TypeNode> containers = query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.FIELD_TYPE)
                .map(Edge::from)
                // The FIELD_TYPE edge comes from a FieldNode, so find its declaring type
                .map(fieldId -> query.graph().indexes().declaringTypeOf(fieldId))
                .flatMap(Optional::stream)
                .map(query::type)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        if (containers.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Filter to keep only aggregate-like containers (types with identity)
        List<TypeNode> aggregateContainers =
                containers.stream().filter(c -> hasIdentityField(c, query)).toList();

        if (aggregateContainers.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Check if this type is immutable (record or has only final fields)
        if (!isImmutable(node, query)) {
            // Still match but with lower confidence
            String containerNames =
                    aggregateContainers.stream().map(TypeNode::simpleName).collect(joining(", "));

            return MatchResult.match(
                    ConfidenceLevel.MEDIUM,
                    "Embedded in aggregate(s) [%s], no identity, but not immutable".formatted(containerNames),
                    List.of(
                            Evidence.fromRelationship(
                                    "Embedded in: " + containerNames,
                                    aggregateContainers.stream()
                                            .map(TypeNode::id)
                                            .toList()),
                            Evidence.fromStructure("No identity field", List.of(node.id()))));
        }

        String containerNames =
                aggregateContainers.stream().map(TypeNode::simpleName).collect(joining(", "));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Immutable type embedded in aggregate(s): " + containerNames,
                List.of(
                        Evidence.fromRelationship(
                                "Embedded in: " + containerNames,
                                aggregateContainers.stream().map(TypeNode::id).toList()),
                        Evidence.fromStructure("No identity, immutable", List.of(node.id()))));
    }

    private boolean hasIdentityField(TypeNode type, GraphQuery query) {
        return query.fieldsOf(type).stream().anyMatch(this::isIdentityField);
    }

    private boolean isIdentityField(FieldNode field) {
        String name = field.simpleName();
        return name.equals("id") || name.endsWith("Id");
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
}
