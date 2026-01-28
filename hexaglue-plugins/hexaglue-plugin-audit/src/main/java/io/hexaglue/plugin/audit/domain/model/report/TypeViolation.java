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
 * Represents a violation associated with a specific type for diagram visualization.
 *
 * @param typeName simple name of the affected type
 * @param violationType type of violation for styling purposes
 * @since 5.0.0
 */
public record TypeViolation(String typeName, ViolationType violationType) {

    /**
     * Types of violations that can be visualized in diagrams.
     *
     * <p>Each type maps to a specific visual style in class diagrams:
     * <ul>
     *   <li>DDD violations: CYCLE, MUTABLE_VALUE_OBJECT, IMPURE_DOMAIN, BOUNDARY_VIOLATION,
     *       MISSING_IDENTITY, MISSING_REPOSITORY, EVENT_NAMING</li>
     *   <li>Hexagonal violations: PORT_UNCOVERED, DEPENDENCY_INVERSION, LAYER_VIOLATION,
     *       PORT_NOT_INTERFACE</li>
     * </ul>
     *
     * @since 5.0.0
     */
    public enum ViolationType {
        // DDD violations (existing)
        /** Cycle between aggregates - red alert style. */
        CYCLE,
        /** Mutable value object - orange warning style. */
        MUTABLE_VALUE_OBJECT,
        /** Domain purity violation (infrastructure imports) - purple style. */
        IMPURE_DOMAIN,
        /** Aggregate boundary violation - red style affects relationships. */
        BOUNDARY_VIOLATION,

        // DDD violations (new)
        /** Entity missing identity field - yellow warning style. @since 5.0.0 */
        MISSING_IDENTITY,
        /** Aggregate root missing repository - blue info style. @since 5.0.0 */
        MISSING_REPOSITORY,
        /** Domain event not named in past tense - cyan style. @since 5.0.0 */
        EVENT_NAMING,

        // Hexagonal violations (new)
        /** Port without adapter implementation - teal style. @since 5.0.0 */
        PORT_UNCOVERED,
        /** Dependency inversion principle violated - amber style. @since 5.0.0 */
        DEPENDENCY_INVERSION,
        /** Layer isolation violated - gray style. @since 5.0.0 */
        LAYER_VIOLATION,
        /** Port is not an interface - brown style. @since 5.0.0 */
        PORT_NOT_INTERFACE
    }

    public TypeViolation {
        Objects.requireNonNull(typeName, "typeName is required");
        Objects.requireNonNull(violationType, "violationType is required");
    }

    /**
     * Creates a mutable value object violation.
     */
    public static TypeViolation mutableValueObject(String typeName) {
        return new TypeViolation(typeName, ViolationType.MUTABLE_VALUE_OBJECT);
    }

    /**
     * Creates a domain purity violation.
     */
    public static TypeViolation impureDomain(String typeName) {
        return new TypeViolation(typeName, ViolationType.IMPURE_DOMAIN);
    }

    /**
     * Creates a boundary violation.
     */
    public static TypeViolation boundaryViolation(String typeName) {
        return new TypeViolation(typeName, ViolationType.BOUNDARY_VIOLATION);
    }

    /**
     * Creates a cycle violation.
     */
    public static TypeViolation cycle(String typeName) {
        return new TypeViolation(typeName, ViolationType.CYCLE);
    }

    // --- New DDD factory methods (since 5.0.0) ---

    /**
     * Creates a missing identity violation.
     *
     * <p>Indicates an entity that is missing a proper identity field.
     *
     * @param typeName the entity type name
     * @return a TypeViolation with MISSING_IDENTITY type
     * @since 5.0.0
     */
    public static TypeViolation missingIdentity(String typeName) {
        return new TypeViolation(typeName, ViolationType.MISSING_IDENTITY);
    }

    /**
     * Creates a missing repository violation.
     *
     * <p>Indicates an aggregate root that has no associated repository.
     *
     * @param typeName the aggregate root type name
     * @return a TypeViolation with MISSING_REPOSITORY type
     * @since 5.0.0
     */
    public static TypeViolation missingRepository(String typeName) {
        return new TypeViolation(typeName, ViolationType.MISSING_REPOSITORY);
    }

    /**
     * Creates an event naming violation.
     *
     * <p>Indicates a domain event not following past tense naming convention.
     *
     * @param typeName the event type name
     * @return a TypeViolation with EVENT_NAMING type
     * @since 5.0.0
     */
    public static TypeViolation eventNaming(String typeName) {
        return new TypeViolation(typeName, ViolationType.EVENT_NAMING);
    }

    // --- New hexagonal factory methods (since 5.0.0) ---

    /**
     * Creates a port uncovered violation.
     *
     * <p>Indicates a port without any adapter implementation.
     *
     * @param typeName the port type name
     * @return a TypeViolation with PORT_UNCOVERED type
     * @since 5.0.0
     */
    public static TypeViolation portUncovered(String typeName) {
        return new TypeViolation(typeName, ViolationType.PORT_UNCOVERED);
    }

    /**
     * Creates a dependency inversion violation.
     *
     * <p>Indicates a violation of the dependency inversion principle.
     *
     * @param typeName the type name
     * @return a TypeViolation with DEPENDENCY_INVERSION type
     * @since 5.0.0
     */
    public static TypeViolation dependencyInversion(String typeName) {
        return new TypeViolation(typeName, ViolationType.DEPENDENCY_INVERSION);
    }

    /**
     * Creates a layer violation.
     *
     * <p>Indicates a violation of layer isolation rules.
     *
     * @param typeName the type name
     * @return a TypeViolation with LAYER_VIOLATION type
     * @since 5.0.0
     */
    public static TypeViolation layerViolation(String typeName) {
        return new TypeViolation(typeName, ViolationType.LAYER_VIOLATION);
    }

    /**
     * Creates a port not interface violation.
     *
     * <p>Indicates a port defined as a class instead of an interface.
     *
     * @param typeName the port type name
     * @return a TypeViolation with PORT_NOT_INTERFACE type
     * @since 5.0.0
     */
    public static TypeViolation portNotInterface(String typeName) {
        return new TypeViolation(typeName, ViolationType.PORT_NOT_INTERFACE);
    }
}
