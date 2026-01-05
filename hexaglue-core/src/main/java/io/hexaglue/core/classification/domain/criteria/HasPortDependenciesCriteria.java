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
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Optional;

/**
 * Matches classes that have dependencies on port interfaces.
 *
 * <p>This criteria detects application services by analyzing the fields
 * of a class. If a class has interface-typed fields (port dependencies),
 * it's likely an application service that orchestrates domain logic
 * with infrastructure through ports.
 *
 * <p>Key distinction from DOMAIN_SERVICE: application services have
 * port dependencies, while domain services don't.
 *
 * <p>Priority: 65 (relationship-based heuristic)
 * <p>Confidence: HIGH
 */
public final class HasPortDependenciesCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "has-port-dependencies";
    }

    @Override
    public int priority() {
        return 65;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.APPLICATION_SERVICE;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a concrete class (not interface, not record, not enum, not abstract)
        if (node.form() != JavaForm.CLASS) {
            return MatchResult.noMatch();
        }

        // Skip abstract classes (they're base classes, not concrete services)
        if (node.isAbstract()) {
            return MatchResult.noMatch();
        }

        // Skip types that look like domain objects (have identity field)
        if (hasIdentityField(node, query)) {
            return MatchResult.noMatch();
        }

        // Find interface-typed fields (port dependencies)
        List<FieldNode> fields = query.fieldsOf(node);
        List<InterfaceField> interfaceFields = fields.stream()
                .map(f -> toInterfaceField(f, query))
                .flatMap(Optional::stream)
                .toList();

        if (interfaceFields.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Check if any of the interface fields are likely ports
        List<InterfaceField> portFields = interfaceFields.stream()
                .filter(this::looksLikePort)
                .toList();

        if (portFields.isEmpty()) {
            return MatchResult.noMatch();
        }

        String portNames = portFields.stream()
                .map(f -> f.type.simpleName())
                .distinct()
                .collect(joining(", "));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Class has port dependencies: " + portNames,
                List.of(Evidence.fromRelationship(
                        "Depends on ports: " + portNames,
                        portFields.stream().map(f -> f.type.id()).toList())));
    }

    private boolean hasIdentityField(TypeNode node, GraphQuery query) {
        return query.fieldsOf(node).stream()
                .anyMatch(f -> f.simpleName().equals("id") || f.simpleName().endsWith("Id"));
    }

    private Optional<InterfaceField> toInterfaceField(FieldNode field, GraphQuery query) {
        // Find the type of this field via FIELD_TYPE edge
        return query.graph().edgesFrom(field.id()).stream()
                .filter(e -> e.kind() == EdgeKind.FIELD_TYPE)
                .findFirst()
                .flatMap(e -> query.type(e.to()))
                .filter(t -> t.form() == JavaForm.INTERFACE)
                .map(t -> new InterfaceField(field, t));
    }

    private boolean looksLikePort(InterfaceField field) {
        String name = field.type.simpleName();
        // Check for common port naming patterns
        return name.endsWith("Repository")
                || name.endsWith("Gateway")
                || name.endsWith("Client")
                || name.endsWith("Service")
                || name.endsWith("Port")
                || name.endsWith("Adapter")
                || name.endsWith("Fetcher")
                || name.endsWith("Saver")
                || name.endsWith("Publisher")
                || name.endsWith("Sender")
                || name.endsWith("Notifier");
    }

    private record InterfaceField(FieldNode field, TypeNode type) {}
}
