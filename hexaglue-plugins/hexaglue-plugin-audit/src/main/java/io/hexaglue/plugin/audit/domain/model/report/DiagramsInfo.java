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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * Metadata about diagrams included in the report.
 * The actual Mermaid content is in DiagramSet, stored separately from JSON.
 *
 * @param c4Context C4 context diagram info
 * @param c4Component C4 component diagram info
 * @param domainModel domain model diagram info
 * @since 5.0.0
 */
public record DiagramsInfo(
        DiagramInfo c4Context,
        DiagramInfo c4Component,
        DiagramInfo domainModel) {

    /**
     * Creates diagrams info with validation.
     */
    public DiagramsInfo {
        Objects.requireNonNull(c4Context, "c4Context is required");
        Objects.requireNonNull(c4Component, "c4Component is required");
        Objects.requireNonNull(domainModel, "domainModel is required");
    }

    /**
     * Creates default diagram information.
     *
     * @return default diagrams info
     */
    public static DiagramsInfo defaults() {
        return new DiagramsInfo(
                new DiagramInfo(
                        "System Context Diagram",
                        "Shows the system with external systems inferred from driven ports"),
                new DiagramInfo(
                        "C4 Component Diagram",
                        "Shows all ports, aggregates, adapters and their relationships"),
                new DiagramInfo("Domain Model", "Shows aggregates with their value objects and inter-aggregate references"));
    }
}
