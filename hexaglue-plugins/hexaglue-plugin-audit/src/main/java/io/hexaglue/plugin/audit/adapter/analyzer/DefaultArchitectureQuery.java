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

package io.hexaglue.plugin.audit.adapter.analyzer;

import io.hexaglue.plugin.audit.adapter.validator.util.CycleDetector;
import io.hexaglue.spi.audit.AggregateInfo;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CouplingMetrics;
import io.hexaglue.spi.audit.CycleKind;
import io.hexaglue.spi.audit.DependencyCycle;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.LayerViolation;
import io.hexaglue.spi.audit.RoleClassification;
import io.hexaglue.spi.audit.StabilityViolation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of ArchitectureQuery.
 *
 * <p>Provides architecture analysis capabilities including cycle detection,
 * metrics calculation, aggregate analysis, and dependency validation.
 *
 * <p>This implementation uses the CycleDetector utility for cycle detection
 * and implements Lakos metrics for dependency analysis.
 *
 * @since 1.0.0
 */
public class DefaultArchitectureQuery implements ArchitectureQuery {

    private final Codebase codebase;
    private final CycleDetector cycleDetector;

    /**
     * Creates a new architecture query instance.
     *
     * @param codebase the codebase to analyze
     */
    public DefaultArchitectureQuery(Codebase codebase) {
        this.codebase = codebase;
        this.cycleDetector = new CycleDetector();
    }

    // === Cycle detection ===

    @Override
    public List<DependencyCycle> findDependencyCycles() {
        Map<String, Set<String>> deps = codebase.dependencies();
        Set<String> allTypes =
                codebase.units().stream().map(CodeUnit::qualifiedName).collect(Collectors.toSet());

        List<List<String>> cycles = cycleDetector.findCycles(allTypes, deps);

        return cycles.stream()
                .map(cycle -> new DependencyCycle(CycleKind.TYPE_LEVEL, cycle))
                .toList();
    }

    @Override
    public List<DependencyCycle> findPackageCycles() {
        // Group types by package
        Map<String, Set<String>> packageDeps = buildPackageDependencyGraph();

        List<List<String>> cycles = cycleDetector.findCycles(packageDeps.keySet(), packageDeps);

        return cycles.stream()
                .map(cycle -> new DependencyCycle(CycleKind.PACKAGE_LEVEL, cycle))
                .toList();
    }

    @Override
    public List<DependencyCycle> findBoundedContextCycles() {
        // Simplified: not implemented for MVP
        return List.of();
    }

    // === Lakos metrics ===

    @Override
    public int calculateDependsOnScore(String qualifiedName) {
        Set<String> visited = new HashSet<>();
        return calculateTransitiveDependencies(qualifiedName, visited);
    }

    @Override
    public int calculateCCD(String packageName) {
        List<CodeUnit> typesInPackage = codebase.units().stream()
                .filter(u -> u.packageName().equals(packageName))
                .toList();

        return typesInPackage.stream()
                .mapToInt(u -> calculateDependsOnScore(u.qualifiedName()))
                .sum();
    }

    @Override
    public double calculateNCCD(String packageName) {
        List<CodeUnit> typesInPackage = codebase.units().stream()
                .filter(u -> u.packageName().equals(packageName))
                .toList();

        int n = typesInPackage.size();
        if (n == 0) {
            return 0.0;
        }

        int ccd = calculateCCD(packageName);
        return (double) ccd / (n * n);
    }

    // === Aggregate analysis ===

    @Override
    public List<AggregateInfo> findAggregates() {
        List<CodeUnit> aggregateRoots = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        return aggregateRoots.stream().map(this::toAggregateInfo).toList();
    }

    @Override
    public List<String> findEntitiesInAggregate(String aggregateRootType) {
        Set<String> deps = codebase.dependencies().getOrDefault(aggregateRootType, Set.of());

        return deps.stream()
                .flatMap(dep ->
                        codebase.units().stream().filter(u -> u.qualifiedName().equals(dep)))
                .filter(u -> u.role() == RoleClassification.ENTITY)
                .map(CodeUnit::qualifiedName)
                .toList();
    }

    @Override
    public Optional<AggregateInfo> findContainingAggregate(String entityType) {
        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        for (CodeUnit aggregate : aggregates) {
            Set<String> deps = codebase.dependencies().getOrDefault(aggregate.qualifiedName(), Set.of());
            if (deps.contains(entityType)) {
                return Optional.of(toAggregateInfo(aggregate));
            }
        }

        return Optional.empty();
    }

    // === Dependency analysis ===

    @Override
    public List<LayerViolation> findLayerViolations() {
        List<LayerViolation> violations = new ArrayList<>();

        // Check domain → infrastructure violations (forbidden)
        List<CodeUnit> domainTypes = codebase.unitsInLayer(LayerClassification.DOMAIN);

        for (CodeUnit domainType : domainTypes) {
            Set<String> deps = codebase.dependencies().getOrDefault(domainType.qualifiedName(), Set.of());

            for (String dep : deps) {
                CodeUnit depUnit = findUnit(dep);
                if (depUnit != null && depUnit.layer() == LayerClassification.INFRASTRUCTURE) {
                    violations.add(new LayerViolation(
                            domainType.qualifiedName(),
                            dep,
                            "DOMAIN",
                            "INFRASTRUCTURE",
                            "Domain must not depend on Infrastructure"));
                }
            }
        }

        return violations;
    }

    @Override
    public List<StabilityViolation> findStabilityViolations() {
        // Simplified: not implemented for MVP
        return List.of();
    }

    @Override
    public Optional<CouplingMetrics> analyzePackageCoupling(String packageName) {
        List<CodeUnit> typesInPackage = codebase.units().stream()
                .filter(u -> u.packageName().equals(packageName))
                .toList();

        if (typesInPackage.isEmpty()) {
            return Optional.empty();
        }

        // Calculate afferent coupling (incoming dependencies)
        Set<String> packageTypes =
                typesInPackage.stream().map(CodeUnit::qualifiedName).collect(Collectors.toSet());

        int afferent = calculateAfferentCoupling(packageTypes);
        int efferent = calculateEfferentCoupling(typesInPackage, packageTypes);

        double abstractness = 0.0; // Simplified: would need to calculate interface/abstract class ratio

        // CouplingMetrics(packageName, afferentCoupling, efferentCoupling, abstractness)
        return Optional.of(new CouplingMetrics(packageName, afferent, efferent, abstractness));
    }

    @Override
    public List<CouplingMetrics> analyzeAllPackageCoupling() {
        Set<String> packages =
                codebase.units().stream().map(CodeUnit::packageName).collect(Collectors.toSet());

        return packages.stream()
                .map(this::analyzePackageCoupling)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    // === Helper methods ===

    /**
     * Calculates transitive dependencies recursively.
     */
    private int calculateTransitiveDependencies(String typeName, Set<String> visited) {
        if (visited.contains(typeName)) {
            return 0;
        }

        visited.add(typeName);
        Set<String> deps = codebase.dependencies().getOrDefault(typeName, Set.of());

        int count = deps.size();
        for (String dep : deps) {
            count += calculateTransitiveDependencies(dep, visited);
        }

        return count;
    }

    /**
     * Builds package-level dependency graph.
     */
    private Map<String, Set<String>> buildPackageDependencyGraph() {
        Map<String, Set<String>> packageDeps = new HashMap<>();

        for (CodeUnit unit : codebase.units()) {
            String pkg = unit.packageName();
            Set<String> deps = codebase.dependencies().getOrDefault(unit.qualifiedName(), Set.of());

            Set<String> pkgDeps = deps.stream()
                    .map(dep -> findUnit(dep))
                    .filter(depUnit -> depUnit != null)
                    .map(CodeUnit::packageName)
                    .filter(depPkg -> !depPkg.equals(pkg)) // Exclude self
                    .collect(Collectors.toSet());

            packageDeps.computeIfAbsent(pkg, k -> new HashSet<>()).addAll(pkgDeps);
        }

        return packageDeps;
    }

    /**
     * Converts an aggregate root CodeUnit to AggregateInfo.
     */
    private AggregateInfo toAggregateInfo(CodeUnit aggregate) {
        List<String> entities = findEntitiesInAggregate(aggregate.qualifiedName());
        List<String> valueObjects = findValueObjectsInAggregate(aggregate.qualifiedName());
        return new AggregateInfo(aggregate.qualifiedName(), entities, valueObjects);
    }

    /**
     * Finds value objects in an aggregate.
     */
    private List<String> findValueObjectsInAggregate(String aggregateRootType) {
        Set<String> deps = codebase.dependencies().getOrDefault(aggregateRootType, Set.of());

        return deps.stream()
                .flatMap(dep ->
                        codebase.units().stream().filter(u -> u.qualifiedName().equals(dep)))
                .filter(u -> u.role() == RoleClassification.VALUE_OBJECT)
                .map(CodeUnit::qualifiedName)
                .toList();
    }

    /**
     * Finds a CodeUnit by qualified name.
     */
    private CodeUnit findUnit(String qualifiedName) {
        return codebase.units().stream()
                .filter(u -> u.qualifiedName().equals(qualifiedName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Calculates afferent coupling for a set of types.
     */
    private int calculateAfferentCoupling(Set<String> packageTypes) {
        Set<String> incomingTypes = new HashSet<>();

        for (CodeUnit unit : codebase.units()) {
            if (packageTypes.contains(unit.qualifiedName())) {
                continue; // Skip types in the package itself
            }

            Set<String> deps = codebase.dependencies().getOrDefault(unit.qualifiedName(), Set.of());
            if (deps.stream().anyMatch(packageTypes::contains)) {
                incomingTypes.add(unit.qualifiedName());
            }
        }

        return incomingTypes.size();
    }

    /**
     * Calculates efferent coupling for a set of types.
     */
    private int calculateEfferentCoupling(List<CodeUnit> typesInPackage, Set<String> packageTypes) {
        Set<String> outgoingTypes = new HashSet<>();

        for (CodeUnit unit : typesInPackage) {
            Set<String> deps = codebase.dependencies().getOrDefault(unit.qualifiedName(), Set.of());
            outgoingTypes.addAll(
                    deps.stream().filter(dep -> !packageTypes.contains(dep)).toList());
        }

        return outgoingTypes.size();
    }
}
