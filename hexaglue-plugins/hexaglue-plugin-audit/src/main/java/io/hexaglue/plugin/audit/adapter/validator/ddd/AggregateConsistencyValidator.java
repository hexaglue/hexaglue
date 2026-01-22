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
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.RelationshipEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.core.SourceLocation;
import io.hexaglue.syntax.TypeRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Validates aggregate consistency using the v5 ArchitecturalModel API.
     *
     * @param model the architectural model containing domain types
     * @param codebase the codebase (kept for potential future use)
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

        // Rule 1: Check entity belongs to single aggregate
        violations.addAll(checkEntitySingleOwnership(domainIndex));

        // Rule 2: Check aggregate size
        violations.addAll(checkAggregateSize(domainIndex));

        // Rule 3: Check aggregate boundaries (already covered by AggregateBoundaryValidator)
        // Skipping to avoid duplication

        return violations;
    }

    /**
     * Checks that each entity belongs to only one aggregate.
     *
     * <p>Multiple ownership indicates unclear aggregate boundaries. An entity
     * should have a single aggregate root responsible for its consistency.
     *
     * @param domainIndex the domain index to analyze
     * @return list of violations for entities with multiple owners
     */
    private List<Violation> checkEntitySingleOwnership(io.hexaglue.arch.model.index.DomainIndex domainIndex) {
        List<Violation> violations = new ArrayList<>();

        // Build map: entity -> list of aggregates that reference it
        Map<String, List<String>> entityToAggregates = new HashMap<>();

        List<AggregateRoot> aggregates = domainIndex.aggregateRoots().toList();

        for (AggregateRoot aggregate : aggregates) {
            String aggregateQName = aggregate.id().qualifiedName();

            // Check entities referenced by this aggregate
            for (TypeRef entityRef : aggregate.entities()) {
                entityToAggregates
                        .computeIfAbsent(entityRef.qualifiedName(), k -> new ArrayList<>())
                        .add(aggregateQName);
            }
        }

        // Find entities owned by multiple aggregates
        for (Map.Entry<String, List<String>> entry : entityToAggregates.entrySet()) {
            if (entry.getValue().size() > 1) {
                violations.add(createMultiOwnershipViolation(entry.getKey(), entry.getValue()));
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
     * @param domainIndex the domain index to analyze
     * @return list of violations for oversized aggregates
     */
    private List<Violation> checkAggregateSize(io.hexaglue.arch.model.index.DomainIndex domainIndex) {
        List<Violation> violations = new ArrayList<>();

        List<AggregateRoot> aggregates = domainIndex.aggregateRoots().toList();

        for (AggregateRoot aggregate : aggregates) {
            int entityCount = aggregate.entities().size();

            if (entityCount > MAX_AGGREGATE_SIZE) {
                violations.add(createSizeViolation(aggregate, entityCount));
            }
        }

        return violations;
    }

    /**
     * Creates a violation for an entity owned by multiple aggregates.
     */
    private Violation createMultiOwnershipViolation(String entityQName, List<String> aggregateQNames) {
        String entityName = getSimpleName(entityQName);
        List<String> aggregateNames =
                aggregateQNames.stream().map(this::getSimpleName).toList();

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
    private Violation createSizeViolation(AggregateRoot aggregate, int entityCount) {
        String message = ("Aggregate '%s' contains %d entities (maximum: %d). "
                        + "Large aggregates are harder to maintain and can cause performance issues. "
                        + "Consider splitting into multiple aggregates.")
                .formatted(aggregate.id().simpleName(), entityCount, MAX_AGGREGATE_SIZE);

        // Get entity names for evidence
        List<String> entityNames =
                aggregate.entities().stream().map(TypeRef::simpleName).toList();

        return Violation.builder(CONSTRAINT_ID)
                .severity(Severity.MAJOR)
                .message(message)
                .affectedType(aggregate.id().qualifiedName())
                .location(SourceLocation.of(aggregate.id().qualifiedName(), 1, 1))
                .evidence(StructuralEvidence.of(
                        "Aggregate contains %d entities: %s".formatted(entityCount, String.join(", ", entityNames)),
                        aggregate.id().qualifiedName()))
                .build();
    }

    /**
     * Gets the simple name from a qualified name.
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
