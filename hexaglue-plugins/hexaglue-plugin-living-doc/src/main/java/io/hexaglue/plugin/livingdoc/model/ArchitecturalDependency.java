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

package io.hexaglue.plugin.livingdoc.model;

import io.hexaglue.arch.model.graph.RelationType;
import java.util.Objects;

/**
 * Represents an architectural dependency between types.
 *
 * <p>Used to enrich domain documentation with incoming and outgoing
 * relationships extracted from the {@code RelationshipGraph}.
 *
 * @param targetSimpleName the simple name of the related type
 * @param targetQualifiedName the fully qualified name of the related type
 * @param relationType the type of relationship (CONTAINS, REFERENCES, etc.)
 * @param direction whether this dependency is outgoing or incoming
 * @since 5.0.0
 */
public record ArchitecturalDependency(
        String targetSimpleName, String targetQualifiedName, RelationType relationType, Direction direction) {

    /**
     * The direction of an architectural dependency.
     *
     * @since 5.0.0
     */
    public enum Direction {
        /** This type depends on the target. */
        OUTGOING,
        /** The target depends on this type. */
        INCOMING
    }

    /**
     * Compact constructor enforcing non-null constraints.
     */
    public ArchitecturalDependency {
        Objects.requireNonNull(targetSimpleName, "targetSimpleName must not be null");
        Objects.requireNonNull(targetQualifiedName, "targetQualifiedName must not be null");
        Objects.requireNonNull(relationType, "relationType must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
    }
}
