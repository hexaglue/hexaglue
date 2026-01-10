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

import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a violation of an architectural constraint.
 *
 * <p>A violation is an immutable value object that captures:
 * <ul>
 *   <li>Which constraint was violated</li>
 *   <li>The severity of the violation</li>
 *   <li>A descriptive message</li>
 *   <li>The types affected by the violation</li>
 *   <li>The source location where the violation occurred</li>
 *   <li>Evidence supporting the detection</li>
 * </ul>
 *
 * <p>Use the {@link Builder} to construct violations with a fluent API.
 *
 * @param constraintId   the constraint that was violated
 * @param severity       the severity level
 * @param message        the human-readable description
 * @param affectedTypes  the types affected by this violation
 * @param location       the source location
 * @param evidence       the supporting evidence
 * @since 1.0.0
 */
public record Violation(
        ConstraintId constraintId,
        Severity severity,
        String message,
        List<String> affectedTypes,
        SourceLocation location,
        List<Evidence> evidence) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public Violation {
        Objects.requireNonNull(constraintId, "constraintId required");
        Objects.requireNonNull(severity, "severity required");
        Objects.requireNonNull(message, "message required");
        affectedTypes = affectedTypes != null ? List.copyOf(affectedTypes) : List.of();
        Objects.requireNonNull(location, "location required");
        evidence = evidence != null ? List.copyOf(evidence) : List.of();
    }

    /**
     * Creates a new builder for the given constraint.
     *
     * @param constraintId the constraint ID
     * @return a new Builder instance
     */
    public static Builder builder(ConstraintId constraintId) {
        return new Builder(constraintId);
    }

    /**
     * Fluent builder for creating Violation instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final ConstraintId constraintId;
        private Severity severity = Severity.MAJOR;
        private String message = "";
        private final List<String> affectedTypes = new ArrayList<>();
        private SourceLocation location = SourceLocation.of("unknown", 1, 1);
        private final List<Evidence> evidence = new ArrayList<>();

        private Builder(ConstraintId constraintId) {
            this.constraintId = Objects.requireNonNull(constraintId, "constraintId required");
        }

        /**
         * Sets the severity level.
         *
         * @param severity the severity
         * @return this builder
         */
        public Builder severity(Severity severity) {
            this.severity = Objects.requireNonNull(severity, "severity required");
            return this;
        }

        /**
         * Sets the violation message.
         *
         * @param message the message
         * @return this builder
         */
        public Builder message(String message) {
            this.message = Objects.requireNonNull(message, "message required");
            return this;
        }

        /**
         * Adds an affected type.
         *
         * @param qualifiedName the fully qualified type name
         * @return this builder
         */
        public Builder affectedType(String qualifiedName) {
            Objects.requireNonNull(qualifiedName, "qualifiedName required");
            this.affectedTypes.add(qualifiedName);
            return this;
        }

        /**
         * Adds multiple affected types.
         *
         * @param qualifiedNames the fully qualified type names
         * @return this builder
         */
        public Builder affectedTypes(List<String> qualifiedNames) {
            Objects.requireNonNull(qualifiedNames, "qualifiedNames required");
            this.affectedTypes.addAll(qualifiedNames);
            return this;
        }

        /**
         * Sets the source location.
         *
         * @param location the source location
         * @return this builder
         */
        public Builder location(SourceLocation location) {
            this.location = Objects.requireNonNull(location, "location required");
            return this;
        }

        /**
         * Adds evidence supporting this violation.
         *
         * @param evidence the evidence
         * @return this builder
         */
        public Builder evidence(Evidence evidence) {
            Objects.requireNonNull(evidence, "evidence required");
            this.evidence.add(evidence);
            return this;
        }

        /**
         * Adds multiple evidence items.
         *
         * @param evidence the evidence items
         * @return this builder
         */
        public Builder evidence(List<Evidence> evidence) {
            Objects.requireNonNull(evidence, "evidence required");
            this.evidence.addAll(evidence);
            return this;
        }

        /**
         * Builds the Violation instance.
         *
         * @return a new Violation
         * @throws IllegalStateException if message is not set
         */
        public Violation build() {
            if (message.isBlank()) {
                throw new IllegalStateException("message must be set");
            }
            return new Violation(constraintId, severity, message, affectedTypes, location, evidence);
        }
    }
}
