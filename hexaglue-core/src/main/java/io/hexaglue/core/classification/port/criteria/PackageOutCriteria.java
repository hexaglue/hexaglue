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
import io.hexaglue.core.graph.style.PackageOrganizationStyle;
import java.util.List;

/**
 * Matches interfaces in packages indicating outbound/driven ports.
 *
 * <p>Package patterns:
 * <ul>
 *   <li>Package ends with ".out"</li>
 *   <li>Package ends with ".ports.out"</li>
 *   <li>Package ends with ".port.out"</li>
 *   <li>Package contains ".outbound"</li>
 *   <li>Package contains ".secondary"</li>
 * </ul>
 *
 * <p>Confidence and priority are style-aware:
 * <ul>
 *   <li>HEXAGONAL style: priority 60, HIGH confidence (patterns are canonical)</li>
 *   <li>Other styles: priority 60, MEDIUM confidence (patterns are heuristic)</li>
 * </ul>
 *
 * <p>Direction: DRIVEN
 */
public final class PackageOutCriteria implements PortClassificationCriteria {

    @Override
    public String name() {
        return "package-out";
    }

    @Override
    public int priority() {
        return 60;
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

        String packageName = node.packageName();
        String reason = getPackageReason(packageName);

        if (reason == null) {
            return MatchResult.noMatch();
        }

        // Style-aware confidence: HEXAGONAL style gets HIGH confidence because
        // .ports.out patterns are canonical for hexagonal architecture
        PackageOrganizationStyle style = query.packageOrganizationStyle();
        ConfidenceLevel confidence =
                (style == PackageOrganizationStyle.HEXAGONAL) ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM;

        String styleInfo = (style == PackageOrganizationStyle.HEXAGONAL) ? ", hexagonal style detected" : "";

        return MatchResult.match(
                confidence,
                "Interface in package '%s' (%s%s)".formatted(packageName, reason, styleInfo),
                List.of(Evidence.fromPackage(packageName, reason)));
    }

    private String getPackageReason(String packageName) {
        if (packageName.endsWith(".out")) {
            return "ends with '.out'";
        }
        if (packageName.endsWith(".ports.out") || packageName.endsWith(".port.out")) {
            return "ends with '.ports.out' or '.port.out'";
        }
        if (packageName.contains(".outbound") || packageName.endsWith(".outbound")) {
            return "contains '.outbound'";
        }
        if (packageName.contains(".secondary") || packageName.endsWith(".secondary")) {
            return "contains '.secondary'";
        }
        return null;
    }
}
