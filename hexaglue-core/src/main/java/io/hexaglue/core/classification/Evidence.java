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

package io.hexaglue.core.classification;

import io.hexaglue.core.graph.model.NodeId;
import java.util.List;
import java.util.Objects;

/**
 * Evidence supporting a classification decision.
 *
 * <p>Records why a particular classification was made, including
 * related nodes and edges that contributed to the decision.
 *
 * @param type the type of evidence
 * @param description human-readable description of the evidence
 * @param relatedNodes nodes that contributed to this evidence
 */
public record Evidence(EvidenceType type, String description, List<NodeId> relatedNodes) {

    public Evidence {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        relatedNodes = relatedNodes == null ? List.of() : List.copyOf(relatedNodes);
    }

    /**
     * Creates evidence from an annotation.
     */
    public static Evidence fromAnnotation(String annotationName, NodeId annotatedNode) {
        return new Evidence(EvidenceType.ANNOTATION, "Annotated with @" + annotationName, List.of(annotatedNode));
    }

    /**
     * Creates evidence from a naming pattern.
     */
    public static Evidence fromNaming(String pattern, String actualName) {
        return new Evidence(
                EvidenceType.NAMING, "Name '%s' matches pattern '%s'".formatted(actualName, pattern), List.of());
    }

    /**
     * Creates evidence from type structure.
     */
    public static Evidence fromStructure(String description, List<NodeId> relatedNodes) {
        return new Evidence(EvidenceType.STRUCTURE, description, relatedNodes);
    }

    /**
     * Creates evidence from a relationship.
     */
    public static Evidence fromRelationship(String description, List<NodeId> relatedNodes) {
        return new Evidence(EvidenceType.RELATIONSHIP, description, relatedNodes);
    }

    /**
     * Creates evidence from package location.
     */
    public static Evidence fromPackage(String packageName, String reason) {
        return new Evidence(EvidenceType.PACKAGE, "Package '%s' %s".formatted(packageName, reason), List.of());
    }
}
