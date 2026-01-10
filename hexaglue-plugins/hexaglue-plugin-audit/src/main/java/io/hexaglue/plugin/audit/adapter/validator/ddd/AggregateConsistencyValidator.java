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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.RelationshipEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RoleClassification;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates aggregate consistency and boundary rules.
 *
 * <p>DDD Principle: Aggregates are consistency boundaries that protect invariants.
 * This validator ensures proper aggregate design by checking:
 * <ol>
 *   <li><b>Single Ownership</b>: An entity should belong to only ONE aggregate.
 *       Multiple ownership indicates unclear boundaries and potential concurrency issues.</li>
 *   <li><b>Size Limit</b>: Aggregates should not be too large. Large aggregates are
 *       harder to understand, maintain, and can cause performance issues. The default
 *       threshold is 7 entities (configurable).</li>
 *   <li><b>Boundary Respect</b>: External types should not reference internal entities
 *       directly. Only the aggregate root should be accessible from outside the aggregate.</li>
 * </ol>
 *
 * <p><strong>Constraint:</strong> ddd:aggregate-consistency<br>
 * <strong>Severity:</strong> MAJOR<br>
 * <strong>Rationale:</strong> Proper aggregate boundaries are essential for maintaining
 * consistency, preventing concurrency issues, and ensuring clear domain model design.
 *
 * @since 1.0.0
 */
public class AggregateConsistencyValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:aggregate-consistency");
    private static final int MAX_AGGREGATE_SIZE = 7;

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public List<Violation> validate(Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Rule 1: Check entity belongs to single aggregate
        violations.addAll(checkEntitySingleOwnership(codebase));

        // Rule 2: Check aggregate size
        violations.addAll(checkAggregateSize(codebase));

        // Rule 3: Check aggregate boundaries
        violations.addAll(checkAggregateBoundaries(codebase));

        return violations;
    }

    /**
     * Checks that each entity belongs to only one aggregate.
     *
     * <p>Multiple ownership indicates unclear aggregate boundaries. An entity
     * should have a single aggregate root responsible for its consistency.
     *
     * @param codebase the codebase to analyze
     * @return list of violations for entities with multiple owners
     */
    private List<Violation> checkEntitySingleOwnership(Codebase codebase) {
        List<Violation> violations = new ArrayList<>();

        // Build map: entity -> list of aggregates that reference it
        Map<String, List<String>> entityToAggregates = new HashMap<>();

        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        for (CodeUnit aggregate : aggregates) {
            Set<String> deps = codebase.dependencies().getOrDefault(aggregate.qualifiedName(), Set.of());

            for (String dep : deps) {
                // Check if dependency is an entity
                codebase.units().stream()
                        .filter(u -> u.qualifiedName().equals(dep))
                        .filter(u -> u.role() == RoleClassification.ENTITY)
                        .findFirst()
                        .ifPresent(entity -> entityToAggregates
                                .computeIfAbsent(entity.qualifiedName(), k -> new ArrayList<>())
                                .add(aggregate.qualifiedName()));
            }
        }

        // Find entities owned by multiple aggregates
        for (Map.Entry<String, List<String>> entry : entityToAggregates.entrySet()) {
            if (entry.getValue().size() > 1) {
                violations.add(createMultiOwnershipViolation(entry.getKey(), entry.getValue(), codebase));
            }
        }

        return violations;
    }

    /**
     * Checks that aggregates do not exceed the size threshold.
     *
     * <p>Large aggregates are harder to understand and maintain. They can also
     * cause performance issues and transaction boundary problems. The threshold
     * helps identify aggregates that should potentially be split.
     *
     * @param codebase the codebase to analyze
     * @return list of violations for oversized aggregates
     */
    private List<Violation> checkAggregateSize(Codebase codebase) {
        List<Violation> violations = new ArrayList<>();

        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        for (CodeUnit aggregate : aggregates) {
            Set<String> deps = codebase.dependencies().getOrDefault(aggregate.qualifiedName(), Set.of());

            // Count entities within this aggregate
            long entityCount = deps.stream()
                    .flatMap(dep -> codebase.units().stream()
                            .filter(u -> u.qualifiedName().equals(dep)))
                    .filter(u -> u.role() == RoleClassification.ENTITY)
                    .count();

            if (entityCount > MAX_AGGREGATE_SIZE) {
                violations.add(createSizeViolation(aggregate, (int) entityCount, codebase));
            }
        }

        return violations;
    }

    /**
     * Checks that aggregate boundaries are respected.
     *
     * <p>Only the aggregate root should be accessible from outside the aggregate.
     * Internal entities should not be referenced directly by external types,
     * as this breaks encapsulation and can lead to invariant violations.
     *
     * @param codebase the codebase to analyze
     * @return list of violations for boundary breaches
     */
    private List<Violation> checkAggregateBoundaries(Codebase codebase) {
        List<Violation> violations = new ArrayList<>();

        // Build map: aggregate -> set of its internal entities
        Map<String, Set<String>> aggregateEntities = buildAggregateEntityMap(codebase);

        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);
        Set<String> aggregateRoots =
                aggregates.stream().map(CodeUnit::qualifiedName).collect(Collectors.toSet());

        // Get all non-domain types (external to domain model)
        List<CodeUnit> externalTypes = codebase.units().stream()
                .filter(u -> u.layer() != LayerClassification.DOMAIN)
                .toList();

        // Check if external types reference internal entities
        for (CodeUnit external : externalTypes) {
            Set<String> deps = codebase.dependencies().getOrDefault(external.qualifiedName(), Set.of());

            for (String dep : deps) {
                // Skip if referencing aggregate root (allowed)
                if (aggregateRoots.contains(dep)) {
                    continue;
                }

                // Check if this is an internal entity of some aggregate
                for (Map.Entry<String, Set<String>> entry : aggregateEntities.entrySet()) {
                    if (entry.getValue().contains(dep)) {
                        violations.add(createBoundaryViolation(external, dep, entry.getKey(), codebase));
                    }
                }
            }
        }

        return violations;
    }

    /**
     * Builds a map of aggregate root to its internal entities.
     *
     * @param codebase the codebase to analyze
     * @return map of aggregate qualified name to set of entity qualified names
     */
    private Map<String, Set<String>> buildAggregateEntityMap(Codebase codebase) {
        Map<String, Set<String>> aggregateEntities = new HashMap<>();

        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        for (CodeUnit aggregate : aggregates) {
            Set<String> deps = codebase.dependencies().getOrDefault(aggregate.qualifiedName(), Set.of());

            Set<String> entities = deps.stream()
                    .flatMap(dep -> codebase.units().stream()
                            .filter(u -> u.qualifiedName().equals(dep)))
                    .filter(u -> u.role() == RoleClassification.ENTITY)
                    .map(CodeUnit::qualifiedName)
                    .collect(Collectors.toSet());

            aggregateEntities.put(aggregate.qualifiedName(), entities);
        }

        return aggregateEntities;
    }

    /**
     * Creates a violation for an entity owned by multiple aggregates.
     */
    private Violation createMultiOwnershipViolation(
            String entityQName, List<String> aggregateQNames, Codebase codebase) {

        String entityName = getSimpleName(entityQName, codebase);
        List<String> aggregateNames =
                aggregateQNames.stream().map(qn -> getSimpleName(qn, codebase)).toList();

        String message = ("Entity '%s' is referenced by multiple aggregates: %s. "
                        + "An entity should belong to exactly one aggregate.")
                .formatted(entityName, String.join(", ", aggregateNames));

        return Violation.builder(CONSTRAINT_ID)
                .severity(Severity.MAJOR)
                .message(message)
                .affectedType(entityQName)
                .location(SourceLocation.of(entityQName, 1, 1))
                .evidence(RelationshipEvidence.of(
                        "Entity referenced by multiple aggregates",
                        List.of(entityQName),
                        aggregateQNames.stream()
                                .map(aggQName -> aggQName + " -> " + entityQName)
                                .toList()))
                .build();
    }

    /**
     * Creates a violation for an oversized aggregate.
     */
    private Violation createSizeViolation(CodeUnit aggregate, int entityCount, Codebase codebase) {
        String message = ("Aggregate '%s' contains %d entities (maximum: %d). "
                        + "Large aggregates are harder to maintain and can cause performance issues. "
                        + "Consider splitting into multiple aggregates.")
                .formatted(aggregate.simpleName(), entityCount, MAX_AGGREGATE_SIZE);

        // Get entity names for evidence
        Set<String> deps = codebase.dependencies().getOrDefault(aggregate.qualifiedName(), Set.of());
        List<String> entityNames = deps.stream()
                .flatMap(dep ->
                        codebase.units().stream().filter(u -> u.qualifiedName().equals(dep)))
                .filter(u -> u.role() == RoleClassification.ENTITY)
                .map(CodeUnit::simpleName)
                .toList();

        return Violation.builder(CONSTRAINT_ID)
                .severity(Severity.MAJOR)
                .message(message)
                .affectedType(aggregate.qualifiedName())
                .location(SourceLocation.of(aggregate.qualifiedName(), 1, 1))
                .evidence(StructuralEvidence.of(
                        "Aggregate contains %d entities: %s".formatted(entityCount, String.join(", ", entityNames)),
                        aggregate.qualifiedName()))
                .build();
    }

    /**
     * Creates a violation for aggregate boundary breach.
     */
    private Violation createBoundaryViolation(
            CodeUnit external, String entityQName, String aggregateRootQName, Codebase codebase) {

        String externalName = external.simpleName();
        String entityName = getSimpleName(entityQName, codebase);
        String aggregateName = getSimpleName(aggregateRootQName, codebase);

        String message = ("External type '%s' directly references internal entity '%s' of aggregate '%s'. "
                        + "Only the aggregate root should be accessible from outside.")
                .formatted(externalName, entityName, aggregateName);

        return Violation.builder(CONSTRAINT_ID)
                .severity(Severity.MAJOR)
                .message(message)
                .affectedType(external.qualifiedName())
                .location(SourceLocation.of(external.qualifiedName(), 1, 1))
                .evidence(RelationshipEvidence.of(
                        "External type bypassing aggregate root to access internal entity",
                        List.of(external.qualifiedName(), entityQName),
                        List.of(
                                external.qualifiedName() + " -> " + entityQName,
                                "Should use: " + external.qualifiedName() + " -> " + aggregateRootQName)))
                .build();
    }

    /**
     * Gets the simple name of a type from its qualified name.
     */
    private String getSimpleName(String qualifiedName, Codebase codebase) {
        return codebase.units().stream()
                .filter(u -> u.qualifiedName().equals(qualifiedName))
                .map(CodeUnit::simpleName)
                .findFirst()
                .orElse(qualifiedName);
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
