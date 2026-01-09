/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.model;

import java.util.List;

/**
 * Convenience factory for creating dependency-related relationship evidence.
 *
 * <p>This class provides static factory methods for creating {@link RelationshipEvidence}
 * instances specifically for dependency violations. It's a utility class that makes
 * the code more readable when dealing with dependency direction issues.
 *
 * @since 1.0.0
 */
public final class DependencyEvidence {

    private DependencyEvidence() {
        // Utility class
    }

    /**
     * Creates relationship evidence for a dependency violation.
     *
     * @param description the evidence description
     * @param source the source type (depends on target)
     * @param target the target type (depended upon)
     * @return a new RelationshipEvidence instance
     */
    public static RelationshipEvidence of(String description, String source, String target) {
        return RelationshipEvidence.of(
                description, List.of(source, target), List.of(source + " -> " + target));
    }

    /**
     * Creates relationship evidence for a dependency violation with multiple dependencies.
     *
     * @param description the evidence description
     * @param involvedTypes all types involved in the violation
     * @param relationships the specific dependency relationships
     * @return a new RelationshipEvidence instance
     */
    public static RelationshipEvidence of(
            String description, List<String> involvedTypes, List<String> relationships) {
        return RelationshipEvidence.of(description, involvedTypes, relationships);
    }
}
