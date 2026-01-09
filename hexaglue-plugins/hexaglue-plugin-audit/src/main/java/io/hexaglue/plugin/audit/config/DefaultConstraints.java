/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.config;

import io.hexaglue.plugin.audit.adapter.validator.ddd.AggregateConsistencyValidator;
import io.hexaglue.plugin.audit.adapter.validator.ddd.AggregateCycleValidator;
import io.hexaglue.plugin.audit.adapter.validator.ddd.AggregateRepositoryValidator;
import io.hexaglue.plugin.audit.adapter.validator.ddd.EntityIdentityValidator;
import io.hexaglue.plugin.audit.adapter.validator.ddd.ValueObjectImmutabilityValidator;
import io.hexaglue.plugin.audit.adapter.validator.hexagonal.DependencyDirectionValidator;
import io.hexaglue.plugin.audit.adapter.validator.hexagonal.LayerIsolationValidator;
import io.hexaglue.plugin.audit.adapter.validator.hexagonal.PortInterfaceValidator;
import io.hexaglue.plugin.audit.domain.model.Constraint;
import io.hexaglue.plugin.audit.domain.model.Severity;

/**
 * Default DDD constraints provided by the audit plugin.
 *
 * <p>This class serves as a configuration catalog, defining all built-in
 * constraints with their metadata (ID, name, description, severity).
 *
 * <p>The constraints are organized by category:
 * <ul>
 *   <li><strong>DDD Constraints</strong> - Domain-Driven Design principles</li>
 *   <li><strong>Hexagonal Constraints</strong> - Hexagonal architecture rules (future)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class DefaultConstraints {

    private DefaultConstraints() {
        // Utility class
    }

    /**
     * Registers all default constraints with the given registry.
     *
     * @param registry the constraint registry
     */
    public static void registerAll(ConstraintRegistry registry) {
        registerDddConstraints(registry);
        registerHexagonalConstraints(registry);
        // Future: registerQualityConstraints(registry);
    }

    /**
     * Registers DDD-specific constraints.
     */
    private static void registerDddConstraints(ConstraintRegistry registry) {
        // Entity Identity
        registry.register(
                new EntityIdentityValidator(),
                Constraint.ddd(
                        "ddd:entity-identity",
                        "Entities must have identity",
                        "Entities are defined by their identity. Every entity (including aggregate roots) "
                                + "must have a stable identity field that distinguishes it from other instances.",
                        Severity.CRITICAL));

        // Aggregate Repository
        registry.register(
                new AggregateRepositoryValidator(),
                Constraint.ddd(
                        "ddd:aggregate-repository",
                        "Aggregate roots must have repositories",
                        "Aggregates are the unit of retrieval. Each aggregate root should have a dedicated "
                                + "repository interface for persistence and retrieval operations.",
                        Severity.MAJOR));

        // Value Object Immutability
        registry.register(
                new ValueObjectImmutabilityValidator(),
                Constraint.ddd(
                        "ddd:value-object-immutable",
                        "Value objects must be immutable",
                        "Value Objects represent descriptive aspects with no identity. They should be immutable - "
                                + "once created, their state cannot change. This ensures thread-safety and proper value semantics.",
                        Severity.CRITICAL));

        // Aggregate Cycle Detection
        registry.register(
                new AggregateCycleValidator(),
                Constraint.ddd(
                        "ddd:aggregate-cycle",
                        "No circular dependencies between aggregates",
                        "Aggregates are consistency boundaries and must be independent units. "
                                + "Circular dependencies between aggregates indicate poor boundary design and "
                                + "can lead to transaction boundary issues and tight coupling.",
                        Severity.BLOCKER));

        // Aggregate Consistency
        registry.register(
                new AggregateConsistencyValidator(),
                Constraint.ddd(
                        "ddd:aggregate-consistency",
                        "Aggregates must maintain proper boundaries",
                        "Aggregates should have clear boundaries: entities belong to one aggregate, "
                                + "aggregates should not be too large (max 7 entities), and internal entities "
                                + "should not be directly accessible from outside the aggregate.",
                        Severity.MAJOR));
    }

    /**
     * Registers hexagonal architecture constraints.
     */
    private static void registerHexagonalConstraints(ConstraintRegistry registry) {
        // Port Interface
        registry.register(
                new PortInterfaceValidator(),
                Constraint.hexagonal(
                        "hexagonal:port-interface",
                        "Ports must be interfaces",
                        "Ports define the boundaries between the application core and external adapters. "
                                + "They must be interfaces to allow multiple implementations (adapters) and maintain "
                                + "the Dependency Inversion Principle.",
                        Severity.CRITICAL));

        // Dependency Direction
        registry.register(
                new DependencyDirectionValidator(),
                Constraint.hexagonal(
                        "hexagonal:dependency-direction",
                        "Domain must not depend on Infrastructure",
                        "The domain layer (business logic) must not depend on infrastructure concerns. "
                                + "Dependencies should always flow inward toward the domain core. This ensures "
                                + "the domain remains independent and testable.",
                        Severity.BLOCKER));

        // Layer Isolation
        registry.register(
                new LayerIsolationValidator(),
                Constraint.hexagonal(
                        "hexagonal:layer-isolation",
                        "Layers must respect dependency rules",
                        "Layers should have clear boundaries: Domain depends on nothing external, "
                                + "Application depends only on Domain, and Infrastructure can depend on both. "
                                + "This ensures maintainability and flexibility.",
                        Severity.MAJOR));
    }

    /**
     * Returns the list of all default constraints for documentation purposes.
     *
     * @return list of constraint metadata
     */
    public static java.util.List<Constraint> allConstraints() {
        ConstraintRegistry registry = new ConstraintRegistry();
        registerAll(registry);
        return registry.availableConstraints();
    }
}
