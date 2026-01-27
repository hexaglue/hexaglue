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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates that entities within an aggregate are only accessible through the aggregate root.
 *
 * <p>DDD Principle: Aggregates are consistency boundaries. Entities within an aggregate
 * should only be accessible through the aggregate root, never directly from outside code.
 * This ensures that all modifications to the aggregate go through the root, which can
 * enforce invariants and maintain consistency.
 *
 * <p>This validator detects violations where:
 * <ul>
 *   <li>External code (outside the aggregate) directly references an internal entity</li>
 *   <li>Entities should only be accessible through the aggregate root's interface</li>
 * </ul>
 *
 * <p>Aggregate membership is determined by the v5 API via {@link Entity#owningAggregate()},
 * which provides actual type relationships rather than package-based inference.
 *
 * <p><strong>Constraint:</strong> ddd:aggregate-boundary<br>
 * <strong>Severity:</strong> MAJOR<br>
 * <strong>Rationale:</strong> Direct access to internal entities bypasses the aggregate root,
 * breaking encapsulation and potentially violating aggregate invariants.
 *
 * @since 1.0.0
 */
public class AggregateBoundaryValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:aggregate-boundary");

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates aggregate boundaries using the v5 ArchitecturalModel API.
     *
     * @param model the architectural model containing domain types
     * @param codebase the codebase for dependency analysis
     * @param query the architecture query (not used in v5)
     * @return list of violations
     * @since 5.0.0
     */
    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Check if domain index is available
        if (model.domainIndex().isEmpty()) {
            return violations; // Cannot validate without domain index
        }

        var domainIndex = model.domainIndex().get();

        // Get all entities with their owning aggregates
        List<Entity> entities = domainIndex.entities().toList();

        // Check each entity to see if it's referenced from outside its aggregate
        for (Entity entity : entities) {
            String entityQName = entity.id().qualifiedName();

            // Skip entities without an owning aggregate
            if (entity.owningAggregate().isEmpty()) {
                continue;
            }

            String aggregateQName = entity.owningAggregate().get().qualifiedName();

            // Find all code units that depend on this entity
            Set<String> dependents = findDependents(codebase, entityQName);

            // Filter out dependencies from within the same aggregate
            Set<String> externalDependents = filterExternalDependents(dependents, aggregateQName, entities);

            if (!externalDependents.isEmpty()) {
                violations.add(Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.MAJOR)
                        .message("Entity '%s' is accessible outside its aggregate '%s'"
                                .formatted(entity.id().simpleName(), getSimpleName(aggregateQName)))
                        .affectedType(entityQName)
                        .location(SourceLocation.of(entityQName, 1, 1))
                        .evidence(StructuralEvidence.of(
                                "Entities within an aggregate should only be accessible through the aggregate root",
                                entityQName))
                        .build());
            }
        }

        return violations;
    }

    /**
     * Finds all code units that depend on the given target.
     *
     * @param codebase the codebase to search
     * @param targetQName the target qualified name
     * @return set of qualified names that depend on the target
     */
    private Set<String> findDependents(Codebase codebase, String targetQName) {
        Set<String> dependents = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : codebase.dependencies().entrySet()) {
            if (entry.getValue().contains(targetQName)) {
                dependents.add(entry.getKey());
            }
        }

        return dependents;
    }

    /**
     * Filters out dependents that are within the same aggregate.
     *
     * @param dependents all dependents of the entity
     * @param aggregateQName the aggregate that owns the entity
     * @param allEntities all entities for aggregate membership lookup
     * @return dependents that are external to the aggregate
     */
    private Set<String> filterExternalDependents(
            Set<String> dependents, String aggregateQName, List<Entity> allEntities) {

        Set<String> externalDeps = new HashSet<>();

        for (String dependentQName : dependents) {
            // Skip if dependent is the aggregate root itself
            if (dependentQName.equals(aggregateQName)) {
                continue;
            }

            // Skip if dependent is another entity in the same aggregate
            boolean isInSameAggregate = allEntities.stream()
                    .anyMatch(e -> e.id().qualifiedName().equals(dependentQName)
                            && e.owningAggregate()
                                    .map(agg -> agg.qualifiedName().equals(aggregateQName))
                                    .orElse(false));

            if (isInSameAggregate) {
                continue;
            }

            // This is an external dependency
            externalDeps.add(dependentQName);
        }

        return externalDeps;
    }

    /**
     * Extracts the simple name from a qualified name.
     *
     * @param qualifiedName the qualified name
     * @return the simple name
     */
    private String getSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }
}
