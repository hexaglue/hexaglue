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
import static java.util.stream.Collectors.toSet;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Matches interfaces that manipulate aggregate-like types in their signatures.
 *
 * <p>This criteria detects driven (outbound) ports by analyzing the types used
 * in method signatures. If an interface uses types that look like aggregates
 * (have identity fields), it's likely a repository or gateway port.
 *
 * <p>This is a graph-based heuristic that exploits the USES_IN_SIGNATURE edges.
 *
 * <p>Priority: 70 (strong heuristic, below explicit annotations)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVEN
 */
public final class SignatureBasedDrivenPortCriteria implements PortClassificationCriteria, IdentifiedCriteria {

    @Override
    public String id() {
        return "port.signature.drivenPort";
    }

    @Override
    public String name() {
        return "signature-based-driven";
    }

    @Override
    public int priority() {
        return 70;
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

        // Skip interfaces that look like non-repository driven ports (gateways, clients, etc.)
        if (looksLikeNonRepositoryDrivenPort(node)) {
            return MatchResult.noMatch();
        }

        // Find types used in this interface's signatures via USES_IN_SIGNATURE edges
        Set<TypeNode> usedTypes = query.graph().edgesFrom(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.USES_IN_SIGNATURE)
                .map(e -> query.type(e.to()))
                .flatMap(Optional::stream)
                .collect(toSet());

        if (usedTypes.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Find aggregate-like types (types with identity fields)
        List<TypeNode> aggregateLikeTypes =
                usedTypes.stream().filter(t -> hasIdentityField(t, query)).toList();

        if (aggregateLikeTypes.isEmpty()) {
            return MatchResult.noMatch();
        }

        String typeNames = aggregateLikeTypes.stream().map(TypeNode::simpleName).collect(joining(", "));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Interface manipulates aggregate-like types: " + typeNames,
                List.of(Evidence.fromRelationship(
                        "Uses in signature: " + typeNames,
                        aggregateLikeTypes.stream().map(TypeNode::id).toList())));
    }

    private boolean looksLikeDrivingPort(TypeNode node) {
        String name = node.simpleName();
        // Skip use case / command / query patterns
        return name.endsWith("UseCase")
                || name.endsWith("Command")
                || name.endsWith("Query")
                || name.endsWith("Handler")
                || name.endsWith("Service");
    }

    private boolean looksLikeNonRepositoryDrivenPort(TypeNode node) {
        String name = node.simpleName();
        // Skip driven ports that are not repositories (gateway, client, publisher, etc.)
        return name.endsWith("Gateway")
                || name.endsWith("Client")
                || name.endsWith("Publisher")
                || name.endsWith("Sender")
                || name.endsWith("Notifier")
                || name.endsWith("Adapter");
    }

    private boolean hasIdentityField(TypeNode type, GraphQuery query) {
        return query.fieldsOf(type).stream().anyMatch(this::isIdentityField);
    }

    private boolean isIdentityField(FieldNode field) {
        String name = field.simpleName();
        return name.equals("id") || name.endsWith("Id");
    }
}
