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

package io.hexaglue.core.classification.model;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.semantic.InterfaceFacts;
import io.hexaglue.core.classification.semantic.InterfaceFactsIndex;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Matches types that are used as outbound models in DRIVEN port signatures.
 *
 * <p>An outbound model is a type that:
 * <ul>
 *   <li>Appears in method signatures (parameters or return) of DRIVEN port interfaces</li>
 *   <li>Is not itself an interface</li>
 *   <li>Is not a primitive type or common JDK type</li>
 *   <li>Is not already classified as a domain type</li>
 * </ul>
 *
 * <p>Priority: 40 (lower priority, runs after domain classification)
 * <p>Confidence: MEDIUM
 */
public final class OutboundModelCriteria implements ClassificationCriteria<ModelKind> {

    private static final Set<String> EXCLUDED_PACKAGES = Set.of("java.", "javax.", "jakarta.");

    private final InterfaceFactsIndex factsIndex;

    /**
     * Creates the criteria with the given InterfaceFacts index.
     *
     * @param factsIndex the index of interface facts (must not be null)
     */
    public OutboundModelCriteria(InterfaceFactsIndex factsIndex) {
        this.factsIndex = Objects.requireNonNull(factsIndex, "factsIndex cannot be null");
    }

    @Override
    public String name() {
        return "outbound-model";
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public ModelKind targetKind() {
        return ModelKind.OUTBOUND_MODEL;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Skip interfaces - they're ports, not models
        if (node.form() == JavaForm.INTERFACE) {
            return MatchResult.noMatch();
        }

        // Skip JDK types
        if (isExcludedPackage(node.qualifiedName())) {
            return MatchResult.noMatch();
        }

        // Find all driven port interfaces where this type appears in method signatures
        List<NodeId> drivenPortsUsingThisType = findDrivenPortsUsing(node, query);

        if (drivenPortsUsingThisType.isEmpty()) {
            return MatchResult.noMatch();
        }

        return MatchResult.match(
                ConfidenceLevel.MEDIUM,
                "Type appears in %d DRIVEN port signature(s)".formatted(drivenPortsUsingThisType.size()),
                List.of(new Evidence(
                        EvidenceType.RELATIONSHIP,
                        "Used in DRIVEN port method signatures",
                        drivenPortsUsingThisType)));
    }

    private boolean isExcludedPackage(String qualifiedName) {
        return EXCLUDED_PACKAGES.stream().anyMatch(qualifiedName::startsWith);
    }

    private List<NodeId> findDrivenPortsUsing(TypeNode type, GraphQuery query) {
        List<NodeId> result = new ArrayList<>();

        // Get all interfaces
        query.interfaces().forEach(iface -> {
            // Check if this interface is a DRIVEN port candidate
            InterfaceFacts facts = factsIndex.get(iface.id()).orElse(null);
            if (facts == null || !facts.isDrivenPortCandidate()) {
                return;
            }

            // Check if the type appears in this interface's method signatures
            if (typeAppearsInSignature(type, iface, query)) {
                result.add(iface.id());
            }
        });

        return result;
    }

    private boolean typeAppearsInSignature(TypeNode type, TypeNode iface, GraphQuery query) {
        List<MethodNode> methods = query.methodsOf(iface);

        for (MethodNode method : methods) {
            // Check method parameters
            boolean inParameters = query.graph().edgesFrom(method.id()).stream()
                    .filter(e -> e.kind() == EdgeKind.PARAMETER_TYPE)
                    .anyMatch(e -> e.to().equals(type.id()));

            if (inParameters) {
                return true;
            }

            // Check return type
            boolean inReturnType = query.graph().edgesFrom(method.id()).stream()
                    .filter(e -> e.kind() == EdgeKind.RETURN_TYPE)
                    .anyMatch(e -> e.to().equals(type.id()));

            if (inReturnType) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String description() {
        return "Matches types used in DRIVEN port method signatures (OUTBOUND_MODEL)";
    }
}
