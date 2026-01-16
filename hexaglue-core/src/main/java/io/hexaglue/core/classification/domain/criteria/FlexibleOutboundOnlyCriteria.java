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
 * Matches CoreAppClasses that are outbound-only actors.
 *
 * <p>An outbound-only actor is a class that:
 * <ul>
 *   <li>Depends on at least one driven port interface</li>
 *   <li>Implements NO driving port interfaces</li>
 * </ul>
 *
 * <p>Typical examples: background processors, event handlers, scheduled tasks
 * that call infrastructure but are not directly exposed as use cases.
 *
 * <p>Note: If the class has 2+ driven dependencies and state fields,
 * it will be classified as SAGA instead (higher priority).
 *
 * <p>Priority: 68 (semantic heuristic, below INBOUND_ONLY and SAGA)
 * <p>Confidence: HIGH
 */
public final class FlexibleOutboundOnlyCriteria implements ClassificationCriteria<ElementKind>, IdentifiedCriteria {

    private final CoreAppClassIndex coreAppClassIndex;

    /**
     * Creates the criteria with the given CoreAppClass index.
     *
     * @param coreAppClassIndex the index of CoreAppClasses (must not be null)
     */
    public FlexibleOutboundOnlyCriteria(CoreAppClassIndex coreAppClassIndex) {
        this.coreAppClassIndex = Objects.requireNonNull(coreAppClassIndex, "coreAppClassIndex cannot be null");
    }

    @Override
    public String id() {
        return "domain.semantic.outboundOnly";
    }

    @Override
    public String name() {
        return "flexible-outbound-only";
    }

    @Override
    public int priority() {
        return 68;
    }

    @Override
    public ElementKind targetKind() {
        return ElementKind.OUTBOUND_ONLY;
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

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Class depends on %d driven port(s) without implementing driving ports"
                        .formatted(coreApp.dependedInterfaces().size()),
                List.of(new Evidence(
                        EvidenceType.RELATIONSHIP,
                        "Depends on driven ports only",
                        List.copyOf(coreApp.dependedInterfaces()))));
    }

    @Override
    public String description() {
        return "Matches CoreAppClasses that depend on driven ports without implementing driving ports (OUTBOUND_ONLY)";
    }
}
