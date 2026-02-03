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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Matches CoreAppClasses that are application service pivots.
 *
 * <p>An application service pivot is a class that:
 * <ul>
 *   <li>Implements at least one driving port interface</li>
 *   <li>Depends on at least one driven port interface</li>
 * </ul>
 *
 * <p>Typical examples: use case implementations that receive requests
 * through driving ports and delegate to infrastructure via driven ports.
 *
 * <p>Priority: 74 (semantic heuristic, above SAGA and other actor priorities)
 * <p>Confidence: HIGH
 *
 * @since 5.0.0
 */
public final class FlexibleApplicationServiceCriteria
        implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    private final CoreAppClassIndex coreAppClassIndex;

    /**
     * Creates the criteria with the given CoreAppClass index.
     *
     * @param coreAppClassIndex the index of CoreAppClasses (must not be null)
     */
    public FlexibleApplicationServiceCriteria(CoreAppClassIndex coreAppClassIndex) {
        this.coreAppClassIndex = Objects.requireNonNull(coreAppClassIndex, "coreAppClassIndex cannot be null");
    }

    @Override
    public String id() {
        return "domain.semantic.applicationService";
    }

    @Override
    public String name() {
        return "flexible-application-service";
    }

    @Override
    public int priority() {
        return 74;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.APPLICATION_SERVICE;
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

        // Must be a pivot: implements driving ports AND depends on driven ports
        if (!coreApp.isPivot()) {
            return MatchResult.noMatch();
        }

        List<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(
                EvidenceType.RELATIONSHIP,
                "Implements %d driving port(s)"
                        .formatted(coreApp.implementedInterfaces().size()),
                List.copyOf(coreApp.implementedInterfaces())));
        evidences.add(new Evidence(
                EvidenceType.RELATIONSHIP,
                "Depends on %d driven port(s)"
                        .formatted(coreApp.dependedInterfaces().size()),
                List.copyOf(coreApp.dependedInterfaces())));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Class implements %d driving port(s) and depends on %d driven port(s)"
                        .formatted(
                                coreApp.implementedInterfaces().size(),
                                coreApp.dependedInterfaces().size()),
                evidences);
    }

    @Override
    public String description() {
        return "Matches CoreAppClasses that implement driving ports and depend on driven ports (APPLICATION_SERVICE)";
    }
}
