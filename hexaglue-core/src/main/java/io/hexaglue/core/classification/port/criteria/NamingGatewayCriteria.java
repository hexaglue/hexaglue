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
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches interfaces with naming pattern *Gateway, *Client, *Adapter.
 *
 * <p>Naming patterns:
 * <ul>
 *   <li>Name ends with "Gateway"</li>
 *   <li>Name ends with "Client"</li>
 *   <li>Name ends with "Adapter"</li>
 *   <li>Name ends with "Port" (generic secondary port)</li>
 * </ul>
 *
 * <p>Priority: 80 (strong heuristic)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVEN
 */
public final class NamingGatewayCriteria implements PortClassificationCriteria {

    @Override
    public String name() {
        return "naming-gateway";
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public PortKind targetKind() {
        return PortKind.GATEWAY;
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

        String name = node.simpleName();
        String matchedPattern = null;

        if (name.endsWith("Gateway")) {
            matchedPattern = "*Gateway";
        } else if (name.endsWith("Client")) {
            matchedPattern = "*Client";
        } else if (name.endsWith("Adapter")) {
            matchedPattern = "*Adapter";
        } else if (name.endsWith("Port") && !name.equals("Port")) {
            // Matches *Port but not just "Port"
            matchedPattern = "*Port";
        }

        if (matchedPattern == null) {
            return MatchResult.noMatch();
        }

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Interface '%s' matches gateway pattern '%s'".formatted(name, matchedPattern),
                List.of(Evidence.fromNaming(matchedPattern, name)));
    }
}
