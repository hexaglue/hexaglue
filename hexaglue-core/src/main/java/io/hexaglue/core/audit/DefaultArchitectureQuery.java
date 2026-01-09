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

package io.hexaglue.core.audit;

import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.spi.audit.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ArchitectureQuery}.
 *
 * <p>This implementation uses the application graph to analyze architecture
 * and detect violations.
 */
public final class DefaultArchitectureQuery implements ArchitectureQuery {

    private final ApplicationGraph graph;

    public DefaultArchitectureQuery(ApplicationGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
    }

    // === Cycle detection ===

    @Override
    public List<DependencyCycle> findDependencyCycles() {
        List<DependencyCycle> cycles = new ArrayList<>();
        Set<NodeId> visited = new HashSet<>();
        Set<NodeId> recursionStack = new HashSet<>();

        for (TypeNode type : graph.typeNodes()) {
            if (!visited.contains(type.id())) {
                findCyclesHelper(type.id(), visited, recursionStack, new LinkedList<>(), cycles);
            }
        }

        return cycles;
    }

    private void findCyclesHelper(
            NodeId current,
            Set<NodeId> visited,
            Set<NodeId> recursionStack,
            Deque<String> path,
            List<DependencyCycle> cycles) {

        visited.add(current);
        recursionStack.add(current);

        Optional<TypeNode> currentType = graph.typeNode(current);
        if (currentType.isEmpty()) {
            recursionStack.remove(current);
            return;
        }

        path.addLast(currentType.get().qualifiedName());

        // Follow REFERENCES edges (dependency relationships)
        var dependencies = graph.edgesFrom(current).stream()
                .filter(e -> e.kind() == EdgeKind.REFERENCES)
                .map(e -> e.to())
                .toList();

        for (NodeId dependency : dependencies) {
            if (!recursionStack.contains(dependency)) {
                findCyclesHelper(dependency, visited, recursionStack, path, cycles);
            } else {
                // Found a cycle
                Optional<TypeNode> depType = graph.typeNode(dependency);
                if (depType.isPresent()) {
                    List<String> cyclePath =
                            extractCyclePath(path, depType.get().qualifiedName());
                    if (!cyclePath.isEmpty()) {
                        cycles.add(new DependencyCycle(CycleKind.TYPE_LEVEL, cyclePath));
                    }
                }
            }
        }

        path.removeLast();
        recursionStack.remove(current);
    }

    private List<String> extractCyclePath(Deque<String> path, String cycleStart) {
        List<String> cyclePath = new ArrayList<>();
        boolean inCycle = false;

        for (String node : path) {
            if (node.equals(cycleStart)) {
                inCycle = true;
            }
            if (inCycle) {
                cyclePath.add(node);
            }
        }

        if (!cyclePath.isEmpty()) {
            cyclePath.add(cycleStart); // Close the cycle
        }

        return cyclePath;
    }

    @Override
    public List<DependencyCycle> findPackageCycles() {
        // Build package dependency graph
        Map<String, Set<String>> packageDeps = new HashMap<>();

        for (var edge : graph.edges(EdgeKind.REFERENCES)) {
            graph.typeNode(edge.from()).ifPresent(fromType -> {
                graph.typeNode(edge.to()).ifPresent(toType -> {
                    String fromPkg = fromType.packageName();
                    String toPkg = toType.packageName();

                    if (!fromPkg.equals(toPkg)) {
                        packageDeps
                                .computeIfAbsent(fromPkg, k -> new HashSet<>())
                                .add(toPkg);
                    }
                });
            });
        }

        // Find cycles in package graph
        List<DependencyCycle> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String pkg : packageDeps.keySet()) {
            if (!visited.contains(pkg)) {
                findPackageCyclesHelper(pkg, packageDeps, visited, recursionStack, new LinkedList<>(), cycles);
            }
        }

        return cycles;
    }

    private void findPackageCyclesHelper(
            String current,
            Map<String, Set<String>> packageDeps,
            Set<String> visited,
            Set<String> recursionStack,
            Deque<String> path,
            List<DependencyCycle> cycles) {

        visited.add(current);
        recursionStack.add(current);
        path.addLast(current);

        Set<String> dependencies = packageDeps.getOrDefault(current, Set.of());

        for (String dependency : dependencies) {
            if (!recursionStack.contains(dependency)) {
                findPackageCyclesHelper(dependency, packageDeps, visited, recursionStack, path, cycles);
            } else {
                // Found a cycle
                List<String> cyclePath = extractCyclePath(path, dependency);
                if (!cyclePath.isEmpty()) {
                    cycles.add(new DependencyCycle(CycleKind.PACKAGE_LEVEL, cyclePath));
                }
            }
        }

        path.removeLast();
        recursionStack.remove(current);
    }

    @Override
    public List<DependencyCycle> findBoundedContextCycles() {
        // Build bounded context dependency graph
        Map<String, Set<String>> contextDeps = new HashMap<>();

        for (var edge : graph.edges(EdgeKind.REFERENCES)) {
            graph.typeNode(edge.from()).ifPresent(fromType -> {
                graph.typeNode(edge.to()).ifPresent(toType -> {
                    Optional<String> fromContext = extractBoundedContext(fromType.packageName());
                    Optional<String> toContext = extractBoundedContext(toType.packageName());

                    // Only track dependencies between different bounded contexts
                    if (fromContext.isPresent()
                            && toContext.isPresent()
                            && !fromContext.get().equals(toContext.get())) {
                        contextDeps
                                .computeIfAbsent(fromContext.get(), k -> new HashSet<>())
                                .add(toContext.get());
                    }
                });
            });
        }

        // Find cycles in bounded context graph
        List<DependencyCycle> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String context : contextDeps.keySet()) {
            if (!visited.contains(context)) {
                findBoundedContextCyclesHelper(
                        context, contextDeps, visited, recursionStack, new LinkedList<>(), cycles);
            }
        }

        return cycles;
    }

    /**
     * Extracts the bounded context name from a package name.
     *
     * <p>The bounded context is identified as the first subpackage after the common base package.
     * For example:
     * <ul>
     *   <li>"com.example.order.domain" → "order"</li>
     *   <li>"com.example.customer.api" → "customer"</li>
     *   <li>"io.hexaglue.inventory.model" → "inventory"</li>
     * </ul>
     *
     * <p>If the package has fewer than 3 segments (e.g., "com.example"), no bounded context
     * can be reliably determined, and an empty Optional is returned.
     *
     * @param packageName the fully qualified package name
     * @return the bounded context name, or empty if it cannot be determined
     */
    private Optional<String> extractBoundedContext(String packageName) {
        String[] segments = packageName.split("\\.");

        // Need at least 3 segments to identify a bounded context
        // (e.g., "com.example.order" → "order" is the bounded context)
        if (segments.length < 3) {
            return Optional.empty();
        }

        // The bounded context is typically the third segment (index 2)
        // com.example.order.domain → "order"
        return Optional.of(segments[2]);
    }

    private void findBoundedContextCyclesHelper(
            String current,
            Map<String, Set<String>> contextDeps,
            Set<String> visited,
            Set<String> recursionStack,
            Deque<String> path,
            List<DependencyCycle> cycles) {

        visited.add(current);
        recursionStack.add(current);
        path.addLast(current);

        Set<String> dependencies = contextDeps.getOrDefault(current, Set.of());

        for (String dependency : dependencies) {
            if (!recursionStack.contains(dependency)) {
                findBoundedContextCyclesHelper(dependency, contextDeps, visited, recursionStack, path, cycles);
            } else {
                // Found a cycle
                List<String> cyclePath = extractCyclePath(path, dependency);
                if (!cyclePath.isEmpty()) {
                    cycles.add(new DependencyCycle(CycleKind.BOUNDED_CONTEXT_LEVEL, cyclePath));
                }
            }
        }

        path.removeLast();
        recursionStack.remove(current);
    }

    // === Lakos metrics ===

    @Override
    public int calculateDependsOnScore(String qualifiedName) {
        Optional<TypeNode> type = graph.typeNode(qualifiedName);
        if (type.isEmpty()) {
            return 0;
        }

        Set<NodeId> dependencies = new HashSet<>();
        collectTransitiveDependencies(type.get().id(), dependencies, new HashSet<>());

        return dependencies.size();
    }

    private void collectTransitiveDependencies(NodeId current, Set<NodeId> result, Set<NodeId> visited) {
        if (visited.contains(current)) {
            return;
        }
        visited.add(current);

        var deps = graph.edgesFrom(current).stream()
                .filter(e -> e.kind() == EdgeKind.REFERENCES)
                .map(e -> e.to())
                .filter(NodeId::isType)
                .toList();

        for (NodeId dep : deps) {
            result.add(dep);
            collectTransitiveDependencies(dep, result, visited);
        }
    }

    @Override
    public int calculateCCD(String packageName) {
        var typesInPackage = graph.query().typesInPackage(packageName).toList();

        int ccd = 0;
        for (TypeNode type : typesInPackage) {
            ccd += calculateDependsOnScore(type.qualifiedName());
        }

        return ccd;
    }

    @Override
    public double calculateNCCD(String packageName) {
        var typesInPackage = graph.query().typesInPackage(packageName).toList();
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
        // Aggregate roots are typically identified by having repository interfaces
        // This is a simplified heuristic
        List<AggregateInfo> aggregates = new ArrayList<>();

        for (TypeNode type : graph.typeNodes()) {
            // Check if this type is referenced by a repository
            boolean isAggregateRoot = graph.edgesTo(type.id()).stream().anyMatch(e -> {
                return graph.typeNode(e.from())
                        .map(t -> t.simpleName().endsWith("Repository"))
                        .orElse(false);
            });

            if (isAggregateRoot) {
                List<String> entities = findEntitiesInAggregate(type.qualifiedName());
                List<String> valueObjects = findValueObjectsInAggregate(type.qualifiedName());
                aggregates.add(new AggregateInfo(type.qualifiedName(), entities, valueObjects));
            }
        }

        return aggregates;
    }

    @Override
    public List<String> findEntitiesInAggregate(String aggregateRootType) {
        Optional<TypeNode> root = graph.typeNode(aggregateRootType);
        if (root.isEmpty()) {
            return List.of();
        }

        // Find types directly referenced by the aggregate root (fields and signatures)
        return graph.edgesFrom(root.get().id()).stream()
                .filter(e -> e.kind() == EdgeKind.FIELD_TYPE || e.kind() == EdgeKind.USES_IN_SIGNATURE)
                .map(e -> graph.typeNode(e.to()))
                .flatMap(Optional::stream)
                .filter(t -> !t.simpleName().startsWith("java.")) // Exclude Java standard library
                .map(TypeNode::qualifiedName)
                .distinct()
                .toList();
    }

    private List<String> findValueObjectsInAggregate(String aggregateRootType) {
        Optional<TypeNode> root = graph.typeNode(aggregateRootType);
        if (root.isEmpty()) {
            return List.of();
        }

        // Simplified implementation - would need classification info to properly detect value objects
        return graph.edgesFrom(root.get().id()).stream()
                .filter(e -> e.kind() == EdgeKind.FIELD_TYPE || e.kind() == EdgeKind.USES_IN_SIGNATURE)
                .map(e -> graph.typeNode(e.to()))
                .flatMap(Optional::stream)
                .filter(t -> t.isRecord()) // Records are often value objects
                .map(TypeNode::qualifiedName)
                .distinct()
                .toList();
    }

    @Override
    public Optional<AggregateInfo> findContainingAggregate(String entityType) {
        List<AggregateInfo> aggregates = findAggregates();

        return aggregates.stream().filter(agg -> agg.contains(entityType)).findFirst();
    }

    // === Dependency analysis ===

    @Override
    public List<LayerViolation> findLayerViolations() {
        // Simplified layer violation detection
        // This would need proper layer classification to be accurate
        List<LayerViolation> violations = new ArrayList<>();

        for (var edge : graph.edges(EdgeKind.REFERENCES)) {
            graph.typeNode(edge.from()).ifPresent(fromType -> {
                graph.typeNode(edge.to()).ifPresent(toType -> {
                    String fromLayer = inferLayer(fromType);
                    String toLayer = inferLayer(toType);

                    if (isLayerViolation(fromLayer, toLayer)) {
                        violations.add(new LayerViolation(
                                fromType.qualifiedName(), toType.qualifiedName(), fromLayer, toLayer));
                    }
                });
            });
        }

        return violations;
    }

    private String inferLayer(TypeNode type) {
        String pkg = type.packageName().toLowerCase();

        if (pkg.contains("domain")) return "domain";
        if (pkg.contains("application") || pkg.contains("usecase")) return "application";
        if (pkg.contains("infrastructure") || pkg.contains("adapter")) return "infrastructure";
        if (pkg.contains("presentation") || pkg.contains("controller") || pkg.contains("api")) return "presentation";

        return "unknown";
    }

    private boolean isLayerViolation(String fromLayer, String toLayer) {
        // Domain layer should not depend on infrastructure or application
        if (fromLayer.equals("domain")
                && (toLayer.equals("infrastructure")
                        || toLayer.equals("application")
                        || toLayer.equals("presentation"))) {
            return true;
        }

        // Application layer should not depend on presentation
        if (fromLayer.equals("application") && toLayer.equals("presentation")) {
            return true;
        }

        return false;
    }

    @Override
    public List<StabilityViolation> findStabilityViolations() {
        List<StabilityViolation> violations = new ArrayList<>();
        Map<String, Double> stabilityCache = new HashMap<>();

        for (var edge : graph.edges(EdgeKind.REFERENCES)) {
            graph.typeNode(edge.from()).ifPresent(fromType -> {
                graph.typeNode(edge.to()).ifPresent(toType -> {
                    double fromStability =
                            stabilityCache.computeIfAbsent(fromType.qualifiedName(), this::calculateStability);
                    double toStability =
                            stabilityCache.computeIfAbsent(toType.qualifiedName(), this::calculateStability);

                    // Violation: unstable component depends on more stable component
                    if (fromStability > toStability) {
                        violations.add(new StabilityViolation(
                                fromType.qualifiedName(), toType.qualifiedName(), fromStability, toStability));
                    }
                });
            });
        }

        return violations;
    }

    private double calculateStability(String qualifiedName) {
        Optional<TypeNode> type = graph.typeNode(qualifiedName);
        if (type.isEmpty()) {
            return 0.0;
        }

        int ce = graph.edgesFrom(type.get().id()).stream()
                .filter(e -> e.kind() == EdgeKind.REFERENCES)
                .map(e -> e.to())
                .filter(NodeId::isType)
                .collect(Collectors.toSet())
                .size();

        int ca = graph.edgesTo(type.get().id()).stream()
                .filter(e -> e.kind() == EdgeKind.REFERENCES)
                .map(e -> e.from())
                .filter(NodeId::isType)
                .collect(Collectors.toSet())
                .size();

        int total = ca + ce;
        return total == 0 ? 0.0 : (double) ce / total;
    }

    @Override
    public Optional<CouplingMetrics> analyzePackageCoupling(String packageName) {
        var typesInPackage = graph.query().typesInPackage(packageName).toList();
        if (typesInPackage.isEmpty()) {
            return Optional.empty();
        }

        Set<NodeId> packageTypeIds = typesInPackage.stream().map(TypeNode::id).collect(Collectors.toSet());

        // Calculate afferent coupling (incoming dependencies from other packages)
        int ca = (int) graph.edges(EdgeKind.REFERENCES).stream()
                .filter(e -> !packageTypeIds.contains(e.from()) && packageTypeIds.contains(e.to()))
                .map(e -> e.from())
                .filter(NodeId::isType)
                .distinct()
                .count();

        // Calculate efferent coupling (outgoing dependencies to other packages)
        int ce = (int) graph.edges(EdgeKind.REFERENCES).stream()
                .filter(e -> packageTypeIds.contains(e.from()) && !packageTypeIds.contains(e.to()))
                .map(e -> e.to())
                .filter(NodeId::isType)
                .distinct()
                .count();

        // Calculate abstractness
        long abstractCount = typesInPackage.stream()
                .filter(t -> t.isInterface() || t.isAbstract())
                .count();
        double abstractness = typesInPackage.isEmpty() ? 0.0 : (double) abstractCount / typesInPackage.size();

        return Optional.of(new CouplingMetrics(packageName, ca, ce, abstractness));
    }

    @Override
    public List<CouplingMetrics> analyzeAllPackageCoupling() {
        Set<String> packages =
                graph.typeNodes().stream().map(TypeNode::packageName).collect(Collectors.toSet());

        return packages.stream()
                .map(this::analyzePackageCoupling)
                .flatMap(Optional::stream)
                .toList();
    }
}
