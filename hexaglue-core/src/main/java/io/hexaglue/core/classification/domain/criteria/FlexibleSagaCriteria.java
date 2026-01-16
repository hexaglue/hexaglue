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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.classification.semantic.CoreAppClass;
import io.hexaglue.core.classification.semantic.CoreAppClassIndex;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Matches CoreAppClasses that are sagas (long-running processes).
 *
 * <p>A saga is a class that:
 * <ul>
 *   <li>Is outbound-only (depends on driven ports, no driving implementation)</li>
 *   <li>Depends on 2 or more driven port interfaces</li>
 *   <li>Has state fields for tracking progress</li>
 * </ul>
 *
 * <p>Typical examples: order fulfillment sagas, payment processing sagas,
 * any long-running process that coordinates multiple external systems.
 *
 * <p>Priority: 72 (higher than OUTBOUND_ONLY to take precedence)
 * <p>Confidence: HIGH
 */
public final class FlexibleSagaCriteria implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    private static final int MIN_DRIVEN_DEPENDENCIES = 2;

    private final CoreAppClassIndex coreAppClassIndex;

    /**
     * Creates the criteria with the given CoreAppClass index.
     *
     * @param coreAppClassIndex the index of CoreAppClasses (must not be null)
     */
    public FlexibleSagaCriteria(CoreAppClassIndex coreAppClassIndex) {
        this.coreAppClassIndex = Objects.requireNonNull(coreAppClassIndex, "coreAppClassIndex cannot be null");
    }

    @Override
    public String id() {
        return "domain.semantic.saga";
    }

    @Override
    public String name() {
        return "flexible-saga";
    }

    @Override
    public int priority() {
        return 72;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.SAGA;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a concrete class
        if (node.form() != JavaForm.CLASS) {
            return MatchResult.noMatch();
        }

        // Skip abstract classes
        if (node.isAbstract()) {
            return MatchResult.noMatch();
        }

        // Must be a CoreAppClass
        CoreAppClass coreApp = coreAppClassIndex.get(node.id()).orElse(null);
        if (coreApp == null) {
            return MatchResult.noMatch();
        }

        // Must be outbound-only: depends on driven ports, no driving implementation
        if (!coreApp.isOutboundOnly()) {
            return MatchResult.noMatch();
        }

        // Must have at least 2 driven dependencies
        if (coreApp.dependedInterfaces().size() < MIN_DRIVEN_DEPENDENCIES) {
            return MatchResult.noMatch();
        }

        // Must have state fields (non-interface, non-final mutable fields)
        List<FieldNode> stateFields = findStateFields(node, query);
        if (stateFields.isEmpty()) {
            return MatchResult.noMatch();
        }

        List<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(
                EvidenceType.RELATIONSHIP,
                "Depends on %d driven ports"
                        .formatted(coreApp.dependedInterfaces().size()),
                List.copyOf(coreApp.dependedInterfaces())));
        evidences.add(new Evidence(
                EvidenceType.STRUCTURE,
                "Has %d state field(s): %s"
                        .formatted(
                                stateFields.size(),
                                stateFields.stream().map(FieldNode::simpleName).toList()),
                List.of(node.id())));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Class is a saga with %d driven ports and %d state fields"
                        .formatted(coreApp.dependedInterfaces().size(), stateFields.size()),
                evidences);
    }

    private List<FieldNode> findStateFields(TypeNode node, GraphQuery query) {
        List<FieldNode> fields = query.fieldsOf(node);

        return fields.stream().filter(field -> isStateField(field, query)).toList();
    }

    private boolean isStateField(FieldNode field, GraphQuery query) {
        // Skip final fields (likely dependencies, not state)
        if (field.isFinal()) {
            return false;
        }

        // Skip static fields
        if (field.isStatic()) {
            return false;
        }

        // Check if field type is an interface (likely a dependency, not state)
        boolean isInterfaceTyped = query.graph().edgesFrom(field.id()).stream()
                .filter(e -> e.kind() == EdgeKind.FIELD_TYPE)
                .findFirst()
                .flatMap(e -> query.type(e.to()))
                .map(t -> t.form() == JavaForm.INTERFACE)
                .orElse(false);

        if (isInterfaceTyped) {
            return false;
        }

        // This is a non-final, non-static, non-interface field -> likely state
        return true;
    }

    @Override
    public String description() {
        return "Matches CoreAppClasses that are sagas (outbound-only with 2+ driven dependencies and state fields)";
    }
}
