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
import java.util.Map;
import java.util.Objects;

/**
 * Statistics about the classification results.
 *
 * <p>Provides summary information about how many types were classified,
 * their distribution by kind and confidence level, and conflict counts.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ClassificationStats stats = report.stats();
 *
 * // Check classification coverage
 * double rate = stats.classificationRate();
 * System.out.printf("Classification rate: %.1f%%\n", rate * 100);
 *
 * // Check high confidence rate
 * double highRate = stats.highConfidenceRate();
 * System.out.printf("High confidence: %.1f%%\n", highRate * 100);
 *
 * // Get distribution by kind
 * stats.countByKind().forEach((kind, count) ->
 *     System.out.printf("%s: %d\n", kind, count));
 * }</pre>
 *
 * @param totalTypes the total number of types analyzed
 * @param classifiedTypes the number of types that were successfully classified
 * @param unclassifiedTypes the number of types that could not be classified
 * @param countByKind distribution of types by {@link ArchKind}
 * @param countByConfidence distribution of classified types by {@link ConfidenceLevel}
 * @param conflictCount the number of types that had conflicting classifications
 * @param outOfScopeTypes the number of types classified as OUT_OF_SCOPE (e.g., @Generated types)
 * @since 4.1.0
 */
public record ClassificationStats(
        int totalTypes,
        int classifiedTypes,
        int unclassifiedTypes,
        Map<ArchKind, Integer> countByKind,
        Map<ConfidenceLevel, Integer> countByConfidence,
        int conflictCount,
        int outOfScopeTypes) {

    /**
     * Creates a new ClassificationStats.
     *
     * @param totalTypes the total number of types, must be >= 0
     * @param classifiedTypes the number of classified types, must be >= 0
     * @param unclassifiedTypes the number of unclassified types, must be >= 0
     * @param countByKind distribution by kind, must not be null
     * @param countByConfidence distribution by confidence, must not be null
     * @param conflictCount the number of conflicts, must be >= 0
     * @param outOfScopeTypes the number of out-of-scope types, must be >= 0
     * @throws NullPointerException if countByKind or countByConfidence is null
     * @throws IllegalArgumentException if any count is negative
     */
    public ClassificationStats {
        Objects.requireNonNull(countByKind, "countByKind must not be null");
        Objects.requireNonNull(countByConfidence, "countByConfidence must not be null");
        if (totalTypes < 0) {
            throw new IllegalArgumentException("totalTypes must not be negative");
        }
        if (classifiedTypes < 0) {
            throw new IllegalArgumentException("classifiedTypes must not be negative");
        }
        if (unclassifiedTypes < 0) {
            throw new IllegalArgumentException("unclassifiedTypes must not be negative");
        }
        if (conflictCount < 0) {
            throw new IllegalArgumentException("conflictCount must not be negative");
        }
        if (outOfScopeTypes < 0) {
            throw new IllegalArgumentException("outOfScopeTypes must not be negative");
        }
        countByKind = Map.copyOf(countByKind);
        countByConfidence = Map.copyOf(countByConfidence);
    }

    /**
     * Creates a new ClassificationStats with all parameters (backward-compatible, defaults outOfScopeTypes to 0).
     *
     * @param totalTypes the total number of types analyzed
     * @param classifiedTypes the number of classified types
     * @param unclassifiedTypes the number of unclassified types
     * @param countByKind distribution by kind
     * @param countByConfidence distribution by confidence level
     * @param conflictCount the number of conflicts
     * @return a new ClassificationStats instance
     */
    public static ClassificationStats of(
            int totalTypes,
            int classifiedTypes,
            int unclassifiedTypes,
            Map<ArchKind, Integer> countByKind,
            Map<ConfidenceLevel, Integer> countByConfidence,
            int conflictCount) {
        return new ClassificationStats(
                totalTypes, classifiedTypes, unclassifiedTypes, countByKind, countByConfidence, conflictCount, 0);
    }

    /**
     * Creates a new ClassificationStats with all parameters including out-of-scope count.
     *
     * @param totalTypes the total number of types analyzed
     * @param classifiedTypes the number of classified types
     * @param unclassifiedTypes the number of unclassified types
     * @param countByKind distribution by kind
     * @param countByConfidence distribution by confidence level
     * @param conflictCount the number of conflicts
     * @param outOfScopeTypes the number of out-of-scope types (e.g., @Generated)
     * @return a new ClassificationStats instance
     * @since 5.0.0
     */
    public static ClassificationStats of(
            int totalTypes,
            int classifiedTypes,
            int unclassifiedTypes,
            Map<ArchKind, Integer> countByKind,
            Map<ConfidenceLevel, Integer> countByConfidence,
            int conflictCount,
            int outOfScopeTypes) {
        return new ClassificationStats(
                totalTypes,
                classifiedTypes,
                unclassifiedTypes,
                countByKind,
                countByConfidence,
                conflictCount,
                outOfScopeTypes);
    }

    /**
     * Returns the classification rate as a value between 0.0 and 1.0.
     *
     * <p>This represents the proportion of types that were successfully classified,
     * excluding out-of-scope types (e.g., {@code @Generated} types) from the denominator.
     * A rate of 1.0 means all in-scope types were classified. A rate of 0.0 means no types
     * were classified (or there were no types to classify).</p>
     *
     * @return the classification rate (0.0 to 1.0)
     */
    public double classificationRate() {
        int effectiveTotal = totalTypes - outOfScopeTypes;
        if (effectiveTotal <= 0) {
            return 0.0;
        }
        return (double) classifiedTypes / effectiveTotal;
    }

    /**
     * Returns the rate of high confidence classifications as a value between 0.0 and 1.0.
     *
     * <p>This represents the proportion of classified types that have high confidence.
     * A rate of 1.0 means all classified types have high confidence. A rate of 0.0
     * means no types have high confidence (or there were no classified types).</p>
     *
     * @return the high confidence rate (0.0 to 1.0)
     */
    public double highConfidenceRate() {
        if (classifiedTypes == 0) {
            return 0.0;
        }
        int highCount = countByConfidence.getOrDefault(ConfidenceLevel.HIGH, 0);
        return (double) highCount / classifiedTypes;
    }
}
