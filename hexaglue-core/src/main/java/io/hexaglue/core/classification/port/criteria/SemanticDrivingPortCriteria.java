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
 * Semantic criteria for DRIVING port classification.
 *
 * <p>A DRIVING port is an interface that is <b>implemented</b> by a CoreAppClass.
 * This means the application provides the implementation of this contract.
 *
 * <p>This criteria uses structural analysis via {@link InterfaceFactsIndex}
 * rather than naming conventions.
 *
 * <p>Priority: 85 (higher than naming heuristics, lower than explicit annotations)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVING
 */
public final class SemanticDrivingPortCriteria implements PortClassificationCriteria {

    private final InterfaceFactsIndex factsIndex;

    /**
     * Creates the criteria with the given InterfaceFacts index.
     *
     * @param factsIndex the index of interface facts (must not be null)
     */
    public SemanticDrivingPortCriteria(InterfaceFactsIndex factsIndex) {
        this.factsIndex = Objects.requireNonNull(factsIndex, "factsIndex cannot be null");
    }

    @Override
    public String name() {
        return "semantic-driving";
    }

    @Override
    public int priority() {
        return 85;
    }

    @Override
    public PortKind targetKind() {
        return PortKind.USE_CASE;
    }

    @Override
    public PortDirection targetDirection() {
        return PortDirection.DRIVING;
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

        // DRIVING: Interface is implemented by at least one CoreAppClass
        if (facts.implementedByCore()) {
            return MatchResult.match(
                    ConfidenceLevel.HIGH,
                    "Interface is implemented by CoreAppClass (DRIVING port)",
                    List.of(new Evidence(
                            EvidenceType.RELATIONSHIP,
                            "Implemented by application core - provides this contract",
                            List.of(node.id()))));
        }

        return MatchResult.noMatch();
    }

    @Override
    public String description() {
        return "Matches interfaces implemented by CoreAppClass (DRIVING direction)";
    }
}
