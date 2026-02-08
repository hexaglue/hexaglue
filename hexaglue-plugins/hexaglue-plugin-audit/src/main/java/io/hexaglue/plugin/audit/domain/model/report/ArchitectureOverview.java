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

import java.util.List;
import java.util.Objects;

/**
 * Architecture section of the report.
 *
 * @param summary summary of analyzed architecture
 * @param inventory inventory grouped by bounded context
 * @param components detailed component breakdown
 * @param diagrams diagram metadata (actual Mermaid content is in DiagramSet)
 * @param relationships list of relationships between components
 * @param typeViolations list of type-level violations for diagram visualization
 * @param moduleTopology module topology for multi-module projects (nullable, absent in mono-module)
 * @since 5.0.0
 */
public record ArchitectureOverview(
        String summary,
        Inventory inventory,
        ComponentDetails components,
        DiagramsInfo diagrams,
        List<Relationship> relationships,
        List<TypeViolation> typeViolations,
        ModuleTopology moduleTopology) {

    /**
     * Creates an architecture overview with validation.
     */
    public ArchitectureOverview {
        Objects.requireNonNull(summary, "summary is required");
        Objects.requireNonNull(inventory, "inventory is required");
        Objects.requireNonNull(components, "components is required");
        Objects.requireNonNull(diagrams, "diagrams is required");
        relationships = relationships != null ? List.copyOf(relationships) : List.of();
        typeViolations = typeViolations != null ? List.copyOf(typeViolations) : List.of();
        // moduleTopology is optional - may be null (absent in mono-module)
    }

    /**
     * Constructor for backward compatibility (without typeViolations and moduleTopology).
     */
    public ArchitectureOverview(
            String summary,
            Inventory inventory,
            ComponentDetails components,
            DiagramsInfo diagrams,
            List<Relationship> relationships) {
        this(summary, inventory, components, diagrams, relationships, List.of(), null);
    }

    /**
     * Constructor for backward compatibility (without moduleTopology).
     */
    public ArchitectureOverview(
            String summary,
            Inventory inventory,
            ComponentDetails components,
            DiagramsInfo diagrams,
            List<Relationship> relationships,
            List<TypeViolation> typeViolations) {
        this(summary, inventory, components, diagrams, relationships, typeViolations, null);
    }

    /**
     * Returns the total number of types analyzed.
     *
     * @return total types
     */
    public int totalTypesAnalyzed() {
        return inventory.totals().total();
    }

    /**
     * Returns relationships that are part of cycles.
     *
     * @return cyclic relationships
     */
    public List<Relationship> cycles() {
        return relationships.stream().filter(Relationship::isCycle).toList();
    }
}
