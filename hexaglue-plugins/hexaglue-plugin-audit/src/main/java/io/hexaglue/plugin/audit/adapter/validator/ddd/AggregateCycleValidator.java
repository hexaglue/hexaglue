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
import io.hexaglue.plugin.audit.adapter.validator.util.CycleDetector;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.RelationshipEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates that there are no circular dependencies between aggregate roots.
 *
 * <p>DDD Principle: Aggregates are the consistency boundary and must be
 * independent units. Circular dependencies between aggregates indicate
 * poor boundary design and can lead to:
 * <ul>
 *   <li>Difficulty in understanding the domain model</li>
 *   <li>Problems with transaction boundaries</li>
 *   <li>Challenges in implementing eventual consistency</li>
 *   <li>Tight coupling that prevents independent evolution</li>
 * </ul>
 *
 * <p>This validator uses graph cycle detection to find all cycles in the
 * aggregate dependency graph. Each aggregate root is a node, and edges
 * represent direct dependencies (through references or relations).
 *
 * <p><strong>Constraint:</strong> ddd:aggregate-cycle<br>
 * <strong>Severity:</strong> BLOCKER<br>
 * <strong>Rationale:</strong> Circular aggregate dependencies violate the
 * fundamental principle that aggregates should be consistency boundaries.
 * This is a critical architectural flaw that must be resolved.
 *
 * @since 1.0.0
 */
public class AggregateCycleValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:aggregate-cycle");

    private final CycleDetector cycleDetector;

    /**
     * Creates a new aggregate cycle validator.
     */
    public AggregateCycleValidator() {
        this.cycleDetector = new CycleDetector();
    }

    /**
     * Constructor for testing with a custom cycle detector.
     *
     * @param cycleDetector the cycle detector to use
     */
    AggregateCycleValidator(CycleDetector cycleDetector) {
        this.cycleDetector = cycleDetector;
    }

    @Override
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates aggregate cycles using the v5 ArchitecturalModel API.
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

        // Get all aggregates
        List<AggregateRoot> aggregates = domainIndex.aggregateRoots().toList();

        if (aggregates.isEmpty()) {
            return violations; // No aggregates, no cycles
        }

        // Build set of aggregate qualified names
        Set<String> aggregateNames =
                aggregates.stream().map(agg -> agg.id().qualifiedName()).collect(Collectors.toSet());

        // Build aggregate-only dependency graph
        Map<String, Set<String>> aggregateDeps = buildAggregateDependencyGraph(codebase, aggregateNames);

        // Find cycles using cycle detector
        List<List<String>> cycles = cycleDetector.findCycles(aggregateNames, aggregateDeps);

        // Convert each cycle to a violation
        for (List<String> cycle : cycles) {
            violations.add(cycleToViolation(cycle, aggregates));
        }

        return violations;
    }

    /**
     * Builds a dependency graph containing only aggregate-to-aggregate dependencies.
     *
     * @param codebase the codebase to analyze
     * @param aggregateNames the set of aggregate qualified names
     * @return map of aggregate name to set of aggregates it depends on
     */
    private Map<String, Set<String>> buildAggregateDependencyGraph(Codebase codebase, Set<String> aggregateNames) {

        Map<String, Set<String>> aggregateDeps = new HashMap<>();

        for (String aggregateName : aggregateNames) {
            // Get all dependencies of this aggregate
            Set<String> allDeps = codebase.dependencies().getOrDefault(aggregateName, Set.of());

            // Filter to only aggregate dependencies
            Set<String> aggOnlyDeps =
                    allDeps.stream().filter(aggregateNames::contains).collect(Collectors.toSet());

            aggregateDeps.put(aggregateName, aggOnlyDeps);
        }

        return aggregateDeps;
    }

    /**
     * Converts a cycle path to a Violation.
     *
     * @param cycle the cycle path (list of qualified names)
     * @param aggregates the list of aggregates (for looking up simple names)
     * @return a Violation describing the cycle
     */
    private Violation cycleToViolation(List<String> cycle, List<AggregateRoot> aggregates) {
        // Build map for quick lookup of simple names
        Map<String, String> qnameToSimpleName = aggregates.stream()
                .collect(Collectors.toMap(
                        agg -> agg.id().qualifiedName(), agg -> agg.id().simpleName()));

        // Get simple names for readability
        List<String> simpleNames = cycle.stream()
                .map(qname -> qnameToSimpleName.getOrDefault(qname, qname))
                .toList();

        // Build cycle description
        String cycleDescription = String.join(" -> ", simpleNames);

        // Build relationship strings for evidence
        List<String> relationships = new ArrayList<>();
        for (int i = 0; i < cycle.size() - 1; i++) {
            relationships.add(cycle.get(i) + " -> " + cycle.get(i + 1));
        }

        // The first aggregate in the cycle is the affected type
        String affectedType = cycle.get(0);

        return Violation.builder(CONSTRAINT_ID)
                .severity(Severity.BLOCKER)
                .message("Circular dependency between aggregates: %s".formatted(cycleDescription))
                .affectedType(affectedType)
                .location(SourceLocation.of(affectedType, 1, 1))
                .evidence(RelationshipEvidence.of(
                        "Aggregates must be independent consistency boundaries without circular dependencies",
                        cycle.subList(0, cycle.size() - 1), // Don't duplicate the first element
                        relationships))
                .build();
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }
}
