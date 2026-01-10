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

package io.hexaglue.plugin.audit.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an architectural improvement recommendation based on audit violations.
 *
 * <p>A recommendation groups related violations and provides actionable guidance
 * on how to address architectural issues. Each recommendation includes:
 * <ul>
 *   <li>Priority level for sequencing work</li>
 *   <li>Clear title and detailed description</li>
 *   <li>Affected types to scope the change</li>
 *   <li>Effort estimation for planning</li>
 *   <li>Expected impact to justify the work</li>
 *   <li>Related violations for traceability</li>
 * </ul>
 *
 * <p>This is an immutable value object. Use the {@link Builder} to construct
 * recommendations with a fluent API.
 *
 * @param id                  unique identifier for this recommendation
 * @param priority            the priority level
 * @param title               short summary of the recommendation
 * @param description         detailed explanation and guidance
 * @param affectedTypes       list of fully qualified type names affected
 * @param estimatedEffort     estimated person-days to implement
 * @param expectedImpact      description of the expected improvement
 * @param relatedViolations   list of violation IDs addressed by this recommendation
 * @since 1.0.0
 */
public record Recommendation(
        String id,
        RecommendationPriority priority,
        String title,
        String description,
        List<String> affectedTypes,
        double estimatedEffort,
        String expectedImpact,
        List<ConstraintId> relatedViolations) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public Recommendation {
        Objects.requireNonNull(id, "id required");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        Objects.requireNonNull(priority, "priority required");
        Objects.requireNonNull(title, "title required");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title cannot be blank");
        }
        Objects.requireNonNull(description, "description required");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        affectedTypes = affectedTypes != null ? List.copyOf(affectedTypes) : List.of();
        if (estimatedEffort < 0) {
            throw new IllegalArgumentException("estimatedEffort cannot be negative: " + estimatedEffort);
        }
        Objects.requireNonNull(expectedImpact, "expectedImpact required");
        if (expectedImpact.isBlank()) {
            throw new IllegalArgumentException("expectedImpact cannot be blank");
        }
        relatedViolations = relatedViolations != null ? List.copyOf(relatedViolations) : List.of();
    }

    /**
     * Creates a new builder for constructing recommendations.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for creating Recommendation instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String id = UUID.randomUUID().toString();
        private RecommendationPriority priority = RecommendationPriority.MEDIUM_TERM;
        private String title = "";
        private String description = "";
        private List<String> affectedTypes = List.of();
        private double estimatedEffort = 0.0;
        private String expectedImpact = "";
        private List<ConstraintId> relatedViolations = List.of();

        /**
         * Sets the recommendation ID.
         *
         * @param id the unique identifier
         * @return this builder
         */
        public Builder id(String id) {
            this.id = Objects.requireNonNull(id, "id required");
            return this;
        }

        /**
         * Sets the priority level.
         *
         * @param priority the priority
         * @return this builder
         */
        public Builder priority(RecommendationPriority priority) {
            this.priority = Objects.requireNonNull(priority, "priority required");
            return this;
        }

        /**
         * Sets the recommendation title.
         *
         * @param title the title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = Objects.requireNonNull(title, "title required");
            return this;
        }

        /**
         * Sets the recommendation description.
         *
         * @param description the detailed description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = Objects.requireNonNull(description, "description required");
            return this;
        }

        /**
         * Sets the affected types.
         *
         * @param affectedTypes the list of fully qualified type names
         * @return this builder
         */
        public Builder affectedTypes(List<String> affectedTypes) {
            this.affectedTypes = Objects.requireNonNull(affectedTypes, "affectedTypes required");
            return this;
        }

        /**
         * Sets the estimated effort in person-days.
         *
         * @param estimatedEffort the effort estimate
         * @return this builder
         */
        public Builder estimatedEffort(double estimatedEffort) {
            this.estimatedEffort = estimatedEffort;
            return this;
        }

        /**
         * Sets the expected impact description.
         *
         * @param expectedImpact the impact description
         * @return this builder
         */
        public Builder expectedImpact(String expectedImpact) {
            this.expectedImpact = Objects.requireNonNull(expectedImpact, "expectedImpact required");
            return this;
        }

        /**
         * Sets the related violations.
         *
         * @param relatedViolations the list of constraint IDs
         * @return this builder
         */
        public Builder relatedViolations(List<ConstraintId> relatedViolations) {
            this.relatedViolations = Objects.requireNonNull(relatedViolations, "relatedViolations required");
            return this;
        }

        /**
         * Builds the Recommendation instance.
         *
         * @return a new Recommendation
         * @throws IllegalArgumentException if title, description, or expectedImpact is blank
         * @throws IllegalArgumentException if estimatedEffort is negative
         */
        public Recommendation build() {
            return new Recommendation(
                    id,
                    priority,
                    title,
                    description,
                    affectedTypes,
                    estimatedEffort,
                    expectedImpact,
                    relatedViolations);
        }
    }
}
