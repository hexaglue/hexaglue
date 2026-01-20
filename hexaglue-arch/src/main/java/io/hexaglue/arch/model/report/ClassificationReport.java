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

import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A comprehensive report of the classification process.
 *
 * <p>The report provides statistics, details about unclassified types,
 * conflicts that were resolved, and prioritized remediation suggestions.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ClassificationReport report = model.classificationReport();
 *
 * // Check overall status
 * if (report.hasIssues()) {
 *     System.out.printf("Classification rate: %.1f%%\n",
 *         report.stats().classificationRate() * 100);
 *
 *     // Review items requiring action
 *     for (UnclassifiedType u : report.actionRequired()) {
 *         System.out.printf("%s (%s)\n", u.simpleName(), u.category());
 *     }
 * }
 *
 * // Review top remediations
 * report.remediations().stream()
 *     .sorted()
 *     .limit(5)
 *     .forEach(r -> System.out.printf("[P%d] %s: %s\n",
 *         r.priority(), r.typeName(), r.suggestion()));
 * }</pre>
 *
 * @param stats the classification statistics
 * @param unclassifiedByCategory map of unclassified types by category
 * @param conflicts the conflicts that were detected and resolved
 * @param remediations the prioritized remediation suggestions
 * @param generatedAt when this report was generated
 * @since 4.1.0
 */
public record ClassificationReport(
        ClassificationStats stats,
        Map<UnclassifiedCategory, List<UnclassifiedType>> unclassifiedByCategory,
        List<ClassificationConflict> conflicts,
        List<PrioritizedRemediation> remediations,
        Instant generatedAt) {

    /**
     * Creates a new ClassificationReport.
     *
     * @param stats the statistics, must not be null
     * @param unclassifiedByCategory the unclassified map, must not be null
     * @param conflicts the conflicts, must not be null
     * @param remediations the remediations, must not be null
     * @param generatedAt the timestamp, must not be null
     * @throws NullPointerException if any argument is null
     */
    public ClassificationReport {
        Objects.requireNonNull(stats, "stats must not be null");
        Objects.requireNonNull(unclassifiedByCategory, "unclassifiedByCategory must not be null");
        Objects.requireNonNull(conflicts, "conflicts must not be null");
        Objects.requireNonNull(remediations, "remediations must not be null");
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");

        // Make defensive copies
        unclassifiedByCategory = copyUnclassifiedMap(unclassifiedByCategory);
        conflicts = List.copyOf(conflicts);
        remediations = List.copyOf(remediations);
    }

    private static Map<UnclassifiedCategory, List<UnclassifiedType>> copyUnclassifiedMap(
            Map<UnclassifiedCategory, List<UnclassifiedType>> original) {
        Map<UnclassifiedCategory, List<UnclassifiedType>> copy = new EnumMap<>(UnclassifiedCategory.class);
        original.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    /**
     * Returns unclassified types that require action.
     *
     * <p>Action is required for types with these categories:</p>
     * <ul>
     *   <li>CONFLICTING - conflicting classifications need resolution</li>
     *   <li>AMBIGUOUS - unclear classification needs clarification</li>
     *   <li>UNKNOWN - type needs investigation</li>
     * </ul>
     *
     * <p>Types with UTILITY, OUT_OF_SCOPE, and TECHNICAL categories are
     * intentionally excluded as they typically don't require action.</p>
     *
     * @return list of types requiring action
     */
    public List<UnclassifiedType> actionRequired() {
        return Stream.of(UnclassifiedCategory.CONFLICTING, UnclassifiedCategory.AMBIGUOUS, UnclassifiedCategory.UNKNOWN)
                .flatMap(category -> unclassifiedByCategory.getOrDefault(category, List.of()).stream())
                .toList();
    }

    /**
     * Returns true if there are issues requiring attention.
     *
     * <p>Issues include:</p>
     * <ul>
     *   <li>Unclassified types (stats.unclassifiedTypes > 0)</li>
     *   <li>Classification conflicts (stats.conflictCount > 0)</li>
     * </ul>
     *
     * @return true if issues exist
     */
    public boolean hasIssues() {
        return stats.unclassifiedTypes() > 0 || stats.conflictCount() > 0;
    }

    /**
     * Creates a new builder for constructing a ClassificationReport.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ClassificationReport} instances.
     *
     * @since 4.1.0
     */
    public static final class Builder {
        private ClassificationStats stats;
        private final Map<UnclassifiedCategory, List<UnclassifiedType>> unclassifiedByCategory =
                new EnumMap<>(UnclassifiedCategory.class);
        private final List<ClassificationConflict> conflicts = new ArrayList<>();
        private final List<PrioritizedRemediation> remediations = new ArrayList<>();
        private Instant generatedAt;

        private Builder() {}

        /**
         * Sets the classification statistics.
         *
         * @param stats the statistics
         * @return this builder
         */
        public Builder stats(ClassificationStats stats) {
            this.stats = stats;
            return this;
        }

        /**
         * Adds an unclassified type.
         *
         * @param unclassified the unclassified type
         * @return this builder
         */
        public Builder addUnclassified(UnclassifiedType unclassified) {
            unclassifiedByCategory
                    .computeIfAbsent(unclassified.category(), k -> new ArrayList<>())
                    .add(unclassified);
            return this;
        }

        /**
         * Adds a conflict.
         *
         * @param conflict the conflict
         * @return this builder
         */
        public Builder addConflict(ClassificationConflict conflict) {
            conflicts.add(conflict);
            return this;
        }

        /**
         * Adds a remediation.
         *
         * @param remediation the remediation
         * @return this builder
         */
        public Builder addRemediation(PrioritizedRemediation remediation) {
            remediations.add(remediation);
            return this;
        }

        /**
         * Sets the generation timestamp.
         *
         * @param generatedAt the timestamp
         * @return this builder
         */
        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        /**
         * Builds the ClassificationReport.
         *
         * @return a new ClassificationReport
         * @throws NullPointerException if required fields are not set
         */
        public ClassificationReport build() {
            Objects.requireNonNull(stats, "stats must be set");
            Objects.requireNonNull(generatedAt, "generatedAt must be set");
            return new ClassificationReport(stats, unclassifiedByCategory, conflicts, remediations, generatedAt);
        }
    }
}
