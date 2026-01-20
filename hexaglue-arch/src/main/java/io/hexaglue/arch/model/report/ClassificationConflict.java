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

package io.hexaglue.arch.model.report;

import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.TypeId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a conflict that occurred during classification.
 *
 * <p>A classification conflict occurs when multiple criteria match for the same
 * type with similar priority levels, and the tie-breaking logic had to resolve
 * the ambiguity.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ClassificationConflict conflict = report.conflicts().get(0);
 *
 * System.out.println("Conflict for: " + conflict.typeName());
 * System.out.println("Resolution: " + conflict.resolution());
 *
 * for (ConflictingContribution c : conflict.contributions()) {
 *     System.out.printf("  %s via %s (priority=%d, confidence=%s)\n",
 *         c.kind(), c.criteriaName(), c.priority(), c.confidence());
 * }
 * }</pre>
 *
 * @param typeId the identifier of the type that had conflicting classifications
 * @param typeName the simple name of the type
 * @param contributions the list of conflicting contributions
 * @param resolution the resolution description
 * @param detectedAt when the conflict was detected
 * @since 4.1.0
 */
public record ClassificationConflict(
        TypeId typeId,
        String typeName,
        List<ConflictingContribution> contributions,
        String resolution,
        Instant detectedAt) {

    /**
     * Creates a new ClassificationConflict.
     *
     * @param typeId the type id, must not be null
     * @param typeName the type name, must not be null
     * @param contributions the contributions, must not be null
     * @param resolution the resolution description, must not be null
     * @param detectedAt when detected, must not be null
     * @throws NullPointerException if any argument is null
     */
    public ClassificationConflict {
        Objects.requireNonNull(typeId, "typeId must not be null");
        Objects.requireNonNull(typeName, "typeName must not be null");
        Objects.requireNonNull(contributions, "contributions must not be null");
        Objects.requireNonNull(resolution, "resolution must not be null");
        Objects.requireNonNull(detectedAt, "detectedAt must not be null");
        contributions = List.copyOf(contributions);
    }

    /**
     * Creates a new ClassificationConflict.
     *
     * @param typeId the type id
     * @param typeName the type name
     * @param contributions the conflicting contributions
     * @param resolution the resolution description
     * @param detectedAt when the conflict was detected
     * @return a new ClassificationConflict
     */
    public static ClassificationConflict of(
            TypeId typeId,
            String typeName,
            List<ConflictingContribution> contributions,
            String resolution,
            Instant detectedAt) {
        return new ClassificationConflict(typeId, typeName, contributions, resolution, detectedAt);
    }

    /**
     * Returns true if this represents an actual conflict (more than one contribution).
     *
     * @return true if there are multiple conflicting contributions
     */
    public boolean isRealConflict() {
        return contributions.size() > 1;
    }

    /**
     * Returns the set of distinct {@link ArchKind}s involved in this conflict.
     *
     * @return the set of conflicting kinds
     */
    public Set<ArchKind> conflictingKinds() {
        return contributions.stream().map(ConflictingContribution::kind).collect(Collectors.toSet());
    }

    /**
     * Represents one contribution to a classification conflict.
     *
     * <p>Each contribution represents a criterion that matched and suggested
     * a particular classification for the type.</p>
     *
     * @param kind the architectural kind suggested by this criterion
     * @param criteriaName the name of the criterion that matched
     * @param priority the priority of the criterion
     * @param confidence the confidence level of the match
     * @since 4.1.0
     */
    public record ConflictingContribution(
            ArchKind kind, String criteriaName, int priority, ConfidenceLevel confidence) {

        /**
         * Creates a new ConflictingContribution.
         *
         * @param kind the kind, must not be null
         * @param criteriaName the criteria name, must not be null
         * @param priority the priority
         * @param confidence the confidence, must not be null
         * @throws NullPointerException if any non-primitive argument is null
         */
        public ConflictingContribution {
            Objects.requireNonNull(kind, "kind must not be null");
            Objects.requireNonNull(criteriaName, "criteriaName must not be null");
            Objects.requireNonNull(confidence, "confidence must not be null");
        }

        /**
         * Creates a new ConflictingContribution.
         *
         * @param kind the architectural kind
         * @param criteriaName the criteria name
         * @param priority the priority
         * @param confidence the confidence level
         * @return a new ConflictingContribution
         */
        public static ConflictingContribution of(
                ArchKind kind, String criteriaName, int priority, ConfidenceLevel confidence) {
            return new ConflictingContribution(kind, criteriaName, priority, confidence);
        }
    }
}
