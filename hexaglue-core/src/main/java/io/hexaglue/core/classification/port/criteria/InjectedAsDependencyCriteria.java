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

package io.hexaglue.core.classification.port.criteria;

import static java.util.stream.Collectors.joining;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Matches interfaces that are injected as dependencies in classes.
 *
 * <p>This criteria detects driven (outbound) ports by analyzing how interfaces
 * are used. If an interface is declared as a field in a class, it indicates
 * dependency injection which is characteristic of driven ports (repositories,
 * gateways, clients, etc.).
 *
 * <p>This is a relationship-based heuristic that works regardless of naming
 * conventions or package structure. It captures the semantic essence of a driven
 * port: something that the application depends on for infrastructure concerns.
 *
 * <p>Priority: 75 (strong relationship-based heuristic, below explicit annotations)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVEN
 */
public final class InjectedAsDependencyCriteria implements PortClassificationCriteria {

    @Override
    public String name() {
        return "injected-as-dependency";
    }

    @Override
    public int priority() {
        return 75;
    }

    @Override
    public PortKind targetKind() {
        return PortKind.REPOSITORY;
    }

    @Override
    public PortDirection targetDirection() {
        return PortDirection.DRIVEN;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be an interface
        if (node.form() != JavaForm.INTERFACE) {
            return MatchResult.noMatch();
        }

        // Skip interfaces that look like use cases (driving ports)
        if (looksLikeDrivingPort(node)) {
            return MatchResult.noMatch();
        }

        // Find fields that have this interface as their type
        Set<NodeId> fieldIds = query.graph().indexes().fieldsOfType(node.id());

        if (fieldIds.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Find the declaring types (classes) of those fields
        List<TypeNode> dependentClasses = fieldIds.stream()
                .map(fieldId -> query.graph().indexes().declaringTypeOf(fieldId))
                .flatMap(Optional::stream)
                .distinct()
                .map(query::type)
                .flatMap(Optional::stream)
                .filter(t -> t.form() == JavaForm.CLASS)
                .toList();

        if (dependentClasses.isEmpty()) {
            return MatchResult.noMatch();
        }

        String classNames = dependentClasses.stream()
                .map(TypeNode::simpleName)
                .distinct()
                .collect(joining(", "));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Interface is injected as dependency in: " + classNames,
                List.of(Evidence.fromRelationship(
                        "Injected in classes: " + classNames,
                        dependentClasses.stream().map(TypeNode::id).toList())));
    }

    private boolean looksLikeDrivingPort(TypeNode node) {
        String name = node.simpleName();
        // Skip use case / command / query handler patterns that are driving ports
        return name.endsWith("UseCase")
                || name.endsWith("Command")
                || name.endsWith("Query")
                || name.endsWith("Handler");
    }
}
