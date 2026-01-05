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

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.classification.semantic.InterfaceFacts;
import io.hexaglue.core.classification.semantic.InterfaceFactsIndex;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Objects;

/**
 * Semantic criteria for DRIVEN port classification.
 *
 * <p>A DRIVEN port is an interface that:
 * <ol>
 *   <li>Is <b>used</b> by at least one CoreAppClass (application depends on it)</li>
 *   <li>Has <b>no production implementation</b> (MissingImpl) OR <b>only internal implementations</b> (InternalImplOnly)</li>
 *   <li>Has a <b>jMolecules port annotation</b> (safety check to avoid generating for forgotten interfaces)</li>
 * </ol>
 *
 * <p>This criteria uses structural analysis via {@link InterfaceFactsIndex}
 * rather than naming conventions.
 *
 * <p>Priority: 85 (higher than naming heuristics, lower than explicit annotations)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVEN
 */
public final class SemanticDrivenPortCriteria implements PortClassificationCriteria {

    private final InterfaceFactsIndex factsIndex;

    /**
     * Creates the criteria with the given InterfaceFacts index.
     *
     * @param factsIndex the index of interface facts (must not be null)
     */
    public SemanticDrivenPortCriteria(InterfaceFactsIndex factsIndex) {
        this.factsIndex = Objects.requireNonNull(factsIndex, "factsIndex cannot be null");
    }

    @Override
    public String name() {
        return "semantic-driven";
    }

    @Override
    public int priority() {
        return 85;
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

        // Look up interface facts
        InterfaceFacts facts = factsIndex.get(node.id()).orElse(null);
        if (facts == null) {
            return MatchResult.noMatch();
        }

        // DRIVEN: Interface is used by CoreAppClass AND (MissingImpl OR InternalImplOnly) AND hasPortAnnotation
        if (facts.isDrivenPortCandidate()) {
            String reason = buildReason(facts);
            return MatchResult.match(
                    ConfidenceLevel.HIGH,
                    reason,
                    List.of(new Evidence(
                            EvidenceType.RELATIONSHIP, "Used by application core - consumes this contract", List.of(node.id()))));
        }

        return MatchResult.noMatch();
    }

    private String buildReason(InterfaceFacts facts) {
        StringBuilder sb = new StringBuilder("Interface is used by CoreAppClass");

        if (facts.missingImpl()) {
            sb.append(" with no production implementation");
        } else if (facts.internalImplOnly()) {
            sb.append(" with only internal implementations");
        }

        sb.append(" (DRIVEN port for generation)");
        return sb.toString();
    }

    @Override
    public String description() {
        return "Matches interfaces used by CoreAppClass with missing/internal implementation + port annotation (DRIVEN direction)";
    }
}
