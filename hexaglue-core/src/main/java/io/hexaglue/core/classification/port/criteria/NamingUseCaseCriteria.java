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
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.engine.IdentifiedCriteria;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches interfaces with naming pattern *UseCase.
 *
 * <p>Naming patterns:
 * <ul>
 *   <li>Name ends with "UseCase"</li>
 *   <li>Name ends with "Service" (application service)</li>
 *   <li>Name ends with "Handler" (command/query handler)</li>
 * </ul>
 *
 * <p>Priority: 50 (naming heuristic fallback - demoted from 80)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVING
 *
 * <p><b>Note:</b> This criteria was demoted from priority 80 to 50 to give precedence
 * to semantic criteria ({@link SemanticDrivingPortCriteria}) which use structural analysis.
 */
public final class NamingUseCaseCriteria implements PortClassificationCriteria, IdentifiedCriteria {

    @Override
    public String id() {
        return "port.naming.useCase";
    }

    @Override
    public String name() {
        return "naming-use-case";
    }

    @Override
    public int priority() {
        return 50;
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

        String name = node.simpleName();
        String matchedPattern = null;

        if (name.endsWith("UseCase")) {
            matchedPattern = "*UseCase";
        } else if (name.endsWith("Service") && !name.endsWith("DomainService")) {
            // Exclude DomainService which is a domain concept
            matchedPattern = "*Service";
        } else if (name.endsWith("Handler")) {
            matchedPattern = "*Handler";
        }

        if (matchedPattern == null) {
            return MatchResult.noMatch();
        }

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Interface '%s' matches use case pattern '%s'".formatted(name, matchedPattern),
                List.of(Evidence.fromNaming(matchedPattern, name)));
    }
}
