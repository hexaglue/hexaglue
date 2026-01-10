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

package io.hexaglue.plugin.audit.adapter.metric;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates aggregate boundary encapsulation metrics.
 *
 * <p>This calculator measures what percentage of entities are properly encapsulated
 * within aggregates. An entity is considered "encapsulated" if it belongs to an aggregate
 * (same package as an aggregate root) and is NOT directly referenced by code outside
 * the aggregate boundary.
 *
 * <p>DDD Principle: Entities within an aggregate should only be accessible through
 * the aggregate root. This ensures proper encapsulation and prevents violation of
 * aggregate invariants.
 *
 * <p><strong>Metric:</strong> aggregate.boundary<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if < 80%<br>
 * <strong>Interpretation:</strong> Higher is better. Low values indicate that entities
 * are directly accessed from outside their aggregates, breaking encapsulation.
 *
 * @since 1.0.0
 */
public class AggregateBoundaryMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "aggregate.boundary";
    private static final double WARNING_THRESHOLD = 80.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    @Override
    public Metric calculate(Codebase codebase) {
        List<CodeUnit> entities = codebase.unitsWithRole(RoleClassification.ENTITY);

        // Edge case: no entities means perfect encapsulation
        if (entities.isEmpty()) {
            return Metric.of(
                    METRIC_NAME,
                    100.0,
                    "%",
                    "Percentage of entities properly encapsulated within aggregates (no entities found)");
        }

        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        // Edge case: entities exist but no aggregates means 0% encapsulation
        if (aggregates.isEmpty()) {
            return Metric.of(
                    METRIC_NAME,
                    0.0,
                    "%",
                    "Percentage of entities properly encapsulated within aggregates (no aggregates found)",
                    MetricThreshold.lessThan(WARNING_THRESHOLD));
        }

        // Build entity-to-aggregate membership map
        Map<String, String> entityToAggregate = buildEntityToAggregateMap(aggregates, entities);

        // Count entities that belong to aggregates and check encapsulation
        long entitiesInAggregates = 0;
        long encapsulatedCount = 0;

        for (CodeUnit entity : entities) {
            String entityQName = entity.qualifiedName();
            String aggregateQName = entityToAggregate.get(entityQName);

            // Entity doesn't belong to any aggregate, skip from calculation
            if (aggregateQName == null) {
                continue;
            }

            entitiesInAggregates++;

            // Check if entity is only accessed from within its aggregate
            if (isEncapsulated(entity, aggregateQName, entityToAggregate, codebase)) {
                encapsulatedCount++;
            }
        }

        // Only calculate percentage based on entities that belong to aggregates
        double encapsulationPercentage =
                entitiesInAggregates > 0 ? (double) encapsulatedCount / entitiesInAggregates * 100.0 : 100.0;

        return Metric.of(
                METRIC_NAME,
                encapsulationPercentage,
                "%",
                "Percentage of entities properly encapsulated within aggregates",
                MetricThreshold.lessThan(WARNING_THRESHOLD));
    }

    /**
     * Builds a map of entity qualified names to their owning aggregate root.
     *
     * <p>An entity belongs to an aggregate if it is in the same package as the aggregate
     * or in a sub-package of the aggregate's package.
     *
     * @param aggregates all aggregate roots
     * @param entities all entities
     * @return map of entity qualified name to aggregate qualified name
     */
    private Map<String, String> buildEntityToAggregateMap(List<CodeUnit> aggregates, List<CodeUnit> entities) {
        Map<String, String> map = new HashMap<>();

        for (CodeUnit entity : entities) {
            String entityPackage = entity.packageName();

            // Find the aggregate this entity belongs to
            for (CodeUnit aggregate : aggregates) {
                String aggregatePackage = aggregate.packageName();

                // Entity belongs to aggregate if in same package or sub-package
                if (entityPackage.equals(aggregatePackage) || entityPackage.startsWith(aggregatePackage + ".")) {
                    map.put(entity.qualifiedName(), aggregate.qualifiedName());
                    break; // Assume entity belongs to only one aggregate
                }
            }
        }

        return map;
    }

    /**
     * Checks if an entity is properly encapsulated within its aggregate.
     *
     * <p>An entity is encapsulated if all its dependents are within the aggregate boundary.
     *
     * @param entity the entity to check
     * @param aggregateQName the aggregate that owns the entity
     * @param entityToAggregate map of entities to their aggregates
     * @param codebase the codebase
     * @return true if the entity is properly encapsulated
     */
    private boolean isEncapsulated(
            CodeUnit entity, String aggregateQName, Map<String, String> entityToAggregate, Codebase codebase) {

        String entityQName = entity.qualifiedName();

        // Find all code units that depend on this entity
        Set<String> dependents = findDependents(codebase, entityQName);

        // Check if any dependent is external to the aggregate
        Set<String> externalDependents = filterExternalDependents(dependents, aggregateQName, entityToAggregate);

        // Entity is encapsulated if there are no external dependents
        return externalDependents.isEmpty();
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
     * @param entityToAggregate map of entities to their aggregates
     * @return dependents that are external to the aggregate
     */
    private Set<String> filterExternalDependents(
            Set<String> dependents, String aggregateQName, Map<String, String> entityToAggregate) {

        Set<String> externalDeps = new HashSet<>();
        String aggregatePackage = getPackageName(aggregateQName);

        for (String dependentQName : dependents) {
            // Skip if dependent is the aggregate root itself
            if (dependentQName.equals(aggregateQName)) {
                continue;
            }

            // Skip if dependent is another entity in the same aggregate
            String dependentAggregate = entityToAggregate.get(dependentQName);
            if (aggregateQName.equals(dependentAggregate)) {
                continue;
            }

            // Skip if dependent is in the same package (part of aggregate boundary)
            String dependentPackage = getPackageName(dependentQName);
            if (dependentPackage.equals(aggregatePackage) || dependentPackage.startsWith(aggregatePackage + ".")) {
                continue;
            }

            // This is an external dependency
            externalDeps.add(dependentQName);
        }

        return externalDeps;
    }

    /**
     * Extracts the package name from a qualified name.
     *
     * @param qualifiedName the qualified name
     * @return the package name
     */
    private String getPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
