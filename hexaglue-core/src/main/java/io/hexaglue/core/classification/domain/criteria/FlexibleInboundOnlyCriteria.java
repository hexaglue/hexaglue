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
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Objects;

/**
 * Matches CoreAppClasses that are inbound-only actors.
 *
 * <p>An inbound-only actor is a class that:
 * <ul>
 *   <li>Implements at least one driving port interface</li>
 *   <li>Has NO dependencies on driven port interfaces</li>
 * </ul>
 *
 * <p>Typical examples: query handlers, simple command handlers that
 * don't require infrastructure calls.
 *
 * <p>Priority: 70 (semantic heuristic, below APPLICATION_SERVICE priority)
 * <p>Confidence: HIGH
 */
public final class FlexibleInboundOnlyCriteria implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    private final CoreAppClassIndex coreAppClassIndex;

    /**
     * Creates the criteria with the given CoreAppClass index.
     *
     * @param coreAppClassIndex the index of CoreAppClasses (must not be null)
     */
    public FlexibleInboundOnlyCriteria(CoreAppClassIndex coreAppClassIndex) {
        this.coreAppClassIndex = Objects.requireNonNull(coreAppClassIndex, "coreAppClassIndex cannot be null");
    }

    @Override
    public String id() {
        return "domain.semantic.inboundOnly";
    }

    @Override
    public String name() {
        return "flexible-inbound-only";
    }

    @Override
    public int priority() {
        return 70;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.INBOUND_ONLY;
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

        // Must be inbound-only: implements driving ports, no driven dependencies
        if (!coreApp.isInboundOnly()) {
            return MatchResult.noMatch();
        }

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Class implements %d driving port(s) with no driven dependencies"
                        .formatted(coreApp.implementedInterfaces().size()),
                List.of(new Evidence(
                        EvidenceType.RELATIONSHIP,
                        "Implements driving ports only",
                        List.copyOf(coreApp.implementedInterfaces()))));
    }

    @Override
    public String description() {
        return "Matches CoreAppClasses that implement driving ports without driven dependencies (INBOUND_ONLY)";
    }
}
