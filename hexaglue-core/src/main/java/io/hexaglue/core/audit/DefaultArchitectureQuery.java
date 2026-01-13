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
import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.spi.audit.*;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainModel;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortModel;
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
    private final Map<String, PortDirection> portDirections;
    private final List<Port> ports;
    private final DomainModel domainModel;

    /**
     * Creates a new architecture query with graph only (no port or domain information).
     *
     * @param graph the application graph
     * @deprecated Use {@link #DefaultArchitectureQuery(ApplicationGraph, PortModel, DomainModel)} for full support
     */
    @Deprecated(since = "3.0.0", forRemoval = false)
    public DefaultArchitectureQuery(ApplicationGraph graph) {
        this(graph, null, null);
    }

    /**
     * Creates a new architecture query with graph and port model.
     *
     * @param graph the application graph
     * @param portModel the port model containing classified ports (may be null)
     * @deprecated Use {@link #DefaultArchitectureQuery(ApplicationGraph, PortModel, DomainModel)} for full support
     */
    @Deprecated(since = "3.0.0", forRemoval = false)
    public DefaultArchitectureQuery(ApplicationGraph graph, PortModel portModel) {
        this(graph, portModel, null);
    }

    /**
     * Creates a new architecture query with graph, port model, and domain model.
     *
     * @param graph the application graph
     * @param portModel the port model containing classified ports (may be null)
     * @param domainModel the domain model containing classified domain types (may be null)
     */
    public DefaultArchitectureQuery(ApplicationGraph graph, PortModel portModel, DomainModel domainModel) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
        this.portDirections = buildPortDirectionMap(portModel);
        this.ports = portModel != null && portModel.ports() != null
                ? List.copyOf(portModel.ports())
                : List.of();
        this.domainModel = domainModel;
    }

    private static Map<String, PortDirection> buildPortDirectionMap(PortModel portModel) {
        if (portModel == null || portModel.ports() == null) {
            return Map.of();
        }
        Map<String, PortDirection> map = new HashMap<>();
        for (Port port : portModel.ports()) {
            if (port.direction() != null) {
                map.put(port.qualifiedName(), port.direction());
            }
        }
        return Map.copyOf(map);
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

    @Override
    public List<BoundedContextInfo> findBoundedContexts() {
        // Group types by bounded context
        Map<String, List<String>> contextToTypes = new HashMap<>();
        Map<String, String> contextToRootPackage = new HashMap<>();

        for (TypeNode type : graph.typeNodes()) {
            String packageName = type.packageName();
            Optional<String> contextName = extractBoundedContext(packageName);

            if (contextName.isPresent()) {
                String context = contextName.get();
                contextToTypes.computeIfAbsent(context, k -> new ArrayList<>()).add(type.qualifiedName());

                // Compute the root package (first 3 segments)
                if (!contextToRootPackage.containsKey(context)) {
                    String rootPackage = computeRootPackage(packageName);
                    contextToRootPackage.put(context, rootPackage);
                }
            }
        }

        // Build BoundedContextInfo records
        List<BoundedContextInfo> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : contextToTypes.entrySet()) {
            String contextName = entry.getKey();
            List<String> typeNames = entry.getValue();
            String rootPackage = contextToRootPackage.getOrDefault(contextName, "");

            result.add(new BoundedContextInfo(contextName, rootPackage, typeNames));
        }

        // Sort by context name for deterministic output
        result.sort(Comparator.comparing(BoundedContextInfo::name));

        return result;
    }

    /**
     * Computes the root package for a bounded context (first 3 segments).
     *
     * <p>For example:
     * <ul>
     *   <li>"com.example.order.domain" → "com.example.order"</li>
     *   <li>"com.example.inventory.application.service" → "com.example.inventory"</li>
     * </ul>
     *
     * @param packageName the full package name
     * @return the root package (first 3 segments)
     */
    private String computeRootPackage(String packageName) {
        String[] segments = packageName.split("\\.");
        if (segments.length < 3) {
            return packageName;
        }
        return segments[0] + "." + segments[1] + "." + segments[2];
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

    @Override
    public LakosMetrics calculateLakosMetrics(String packageName) {
        var typesInPackage = graph.query().typesInPackage(packageName).toList();
        if (typesInPackage.isEmpty()) {
            return LakosMetrics.empty();
        }

        Set<String> qualifiedNames =
                typesInPackage.stream().map(TypeNode::qualifiedName).collect(Collectors.toSet());

        return calculateLakosMetrics(qualifiedNames);
    }

    @Override
    public LakosMetrics calculateLakosMetrics(Set<String> qualifiedNames) {
        if (qualifiedNames == null || qualifiedNames.isEmpty()) {
            return LakosMetrics.empty();
        }

        // Collect TypeNodes for the given names
        Set<TypeNode> types = qualifiedNames.stream()
                .map(graph::typeNode)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        if (types.isEmpty()) {
            return LakosMetrics.empty();
        }

        return calculateLakosMetricsForTypes(types);
    }

    @Override
    public LakosMetrics calculateGlobalLakosMetrics() {
        Set<TypeNode> allTypes = new HashSet<>(graph.typeNodes());
        if (allTypes.isEmpty()) {
            return LakosMetrics.empty();
        }

        return calculateLakosMetricsForTypes(allTypes);
    }

    /**
     * Calculates Lakos metrics for a set of types.
     *
     * <p>Algorithm:
     * <pre>
     * 1. For each type, calculate DependsOn (transitive dependencies)
     * 2. CCD = sum of all DependsOn scores
     * 3. ACD = CCD / componentCount
     * 4. NCCD = CCD / (n * log2(n))  [ideal balanced tree]
     * 5. RACD = ACD / log2(n)        [minimum ACD]
     * </pre>
     */
    private LakosMetrics calculateLakosMetricsForTypes(Set<TypeNode> types) {
        int componentCount = types.size();

        // Calculate CCD (Cumulative Component Dependency)
        int ccd = 0;
        for (TypeNode type : types) {
            ccd += calculateDependsOnScore(type.qualifiedName());
        }

        // Calculate ACD (Average Component Dependency)
        double acd = componentCount > 0 ? (double) ccd / componentCount : 0.0;

        // Calculate NCCD (Normalized CCD)
        double nccd = calculateNormalizedCCD(componentCount, ccd);

        // Calculate RACD (Relative ACD)
        double racd = calculateRelativeACD(componentCount, acd);

        return new LakosMetrics(componentCount, ccd, acd, nccd, racd);
    }

    /**
     * Calculates NCCD (Normalized CCD).
     *
     * <p>NCCD compares the actual CCD to the CCD of a balanced binary tree
     * with the same number of nodes. A balanced tree represents an ideal
     * dependency structure.
     *
     * <p>For a balanced binary tree with n nodes:
     * CCD_ideal ≈ n * log₂(n)
     *
     * <p>NCCD = CCD_actual / CCD_ideal
     */
    private double calculateNormalizedCCD(int componentCount, int ccd) {
        if (componentCount <= 1) {
            return 0.0;
        }

        double idealCCD = componentCount * (Math.log(componentCount) / Math.log(2));
        return idealCCD > 0 ? ccd / idealCCD : 0.0;
    }

    /**
     * Calculates RACD (Relative ACD).
     *
     * <p>RACD compares the actual ACD to the theoretical minimum ACD.
     * The minimum occurs when dependencies form a balanced tree.
     *
     * <p>Minimum ACD ≈ log₂(n)
     *
     * <p>RACD = ACD_actual / ACD_min
     */
    private double calculateRelativeACD(int componentCount, double acd) {
        if (componentCount <= 1) {
            return 0.0;
        }

        double minACD = Math.log(componentCount) / Math.log(2);
        return minACD > 0 ? acd / minACD : 0.0;
    }

    // === Aggregate analysis ===

    @Override
    public List<AggregateInfo> findAggregates() {
        // If we have a domain model, use the classified types
        if (domainModel != null) {
            return findAggregatesFromDomainModel();
        }

        // Fallback: use heuristic (deprecated path)
        return findAggregatesFromHeuristic();
    }

    /**
     * Finds aggregates using the classified domain model.
     * This is the preferred method as it uses proper DDD classification.
     */
    private List<AggregateInfo> findAggregatesFromDomainModel() {
        List<AggregateInfo> aggregates = new ArrayList<>();

        // Get all aggregate roots from the domain model
        List<DomainType> aggregateRoots = domainModel.aggregateRoots();

        // Build a map of all domain types for quick lookup
        Map<String, DomainType> typeByName = new HashMap<>();
        for (DomainType type : domainModel.types()) {
            typeByName.put(type.qualifiedName(), type);
        }

        for (DomainType root : aggregateRoots) {
            List<String> entities = new ArrayList<>();
            List<String> valueObjects = new ArrayList<>();

            // Find members of this aggregate by analyzing relationships
            Set<String> members = findAggregateMembersFromRoot(root.qualifiedName(), typeByName);

            for (String memberName : members) {
                DomainType member = typeByName.get(memberName);
                if (member != null) {
                    if (member.kind() == DomainKind.ENTITY) {
                        entities.add(memberName);
                    } else if (member.kind() == DomainKind.VALUE_OBJECT
                            || member.kind() == DomainKind.IDENTIFIER) {
                        valueObjects.add(memberName);
                    }
                }
            }

            aggregates.add(new AggregateInfo(root.qualifiedName(), entities, valueObjects));
        }

        return aggregates;
    }

    /**
     * Finds members of an aggregate by analyzing structural relationships from the root.
     */
    private Set<String> findAggregateMembersFromRoot(String rootQualifiedName, Map<String, DomainType> typeByName) {
        Set<String> members = new HashSet<>();
        Optional<TypeNode> rootNode = graph.typeNode(rootQualifiedName);

        if (rootNode.isEmpty()) {
            return members;
        }

        // Find types directly referenced by the aggregate root
        Set<NodeId> directRefs = graph.edgesFrom(rootNode.get().id()).stream()
                .filter(e -> isStructuralEdge(e.kind()))
                .map(Edge::to)
                .filter(NodeId::isType)
                .collect(Collectors.toSet());

        for (NodeId refId : directRefs) {
            String refName = extractQualifiedNameFromNodeId(refId);
            DomainType refType = typeByName.get(refName);

            // Only include entities and value objects (not other aggregate roots)
            if (refType != null) {
                if (refType.kind() == DomainKind.ENTITY
                        || refType.kind() == DomainKind.VALUE_OBJECT
                        || refType.kind() == DomainKind.IDENTIFIER) {
                    members.add(refName);
                }
            }
        }

        return members;
    }

    /**
     * Fallback: finds aggregates using naming heuristics.
     * This is less accurate than using the domain model.
     */
    private List<AggregateInfo> findAggregatesFromHeuristic() {
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

    // === Port analysis ===

    @Override
    public Optional<PortDirection> findPortDirection(String portQualifiedName) {
        if (portQualifiedName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(portDirections.get(portQualifiedName));
    }

    // === Aggregate membership ===

    @Override
    public Map<String, List<String>> findAggregateMembership() {
        Map<String, List<String>> membership = new HashMap<>();

        List<AggregateInfo> aggregates = findAggregates();
        for (AggregateInfo aggregate : aggregates) {
            // Combine entities and value objects as members
            List<String> members = new ArrayList<>();
            members.addAll(aggregate.entities());
            members.addAll(aggregate.valueObjects());

            if (!members.isEmpty()) {
                membership.put(aggregate.rootType(), members);
            }
        }

        return Map.copyOf(membership);
    }

    @Override
    public Optional<Double> calculateAggregateCohesion(String aggregateRootType) {
        if (aggregateRootType == null) {
            return Optional.empty();
        }

        // Find the aggregate
        Optional<AggregateInfo> aggregateOpt = findAggregates().stream()
                .filter(a -> a.rootType().equals(aggregateRootType))
                .findFirst();

        if (aggregateOpt.isEmpty()) {
            return Optional.empty();
        }

        AggregateInfo aggregate = aggregateOpt.get();

        // Collect all members (root + entities + value objects)
        Set<String> allMembers = new HashSet<>();
        allMembers.add(aggregate.rootType());
        allMembers.addAll(aggregate.entities());
        allMembers.addAll(aggregate.valueObjects());

        int memberCount = allMembers.size();
        if (memberCount <= 1) {
            // Single member aggregate has perfect cohesion
            return Optional.of(1.0);
        }

        // Count internal edges between aggregate members
        int internalEdges = 0;
        for (String member : allMembers) {
            NodeId memberId = NodeId.type(member);
            if (!graph.containsNode(memberId)) {
                continue;
            }

            // Count edges to other members
            for (Edge edge : graph.edgesFrom(memberId)) {
                NodeId targetId = edge.to();
                // Only consider type edges and extract qualified name
                if (targetId.isType()) {
                    String targetType = extractQualifiedNameFromNodeId(targetId);
                    // Only count structural edges (FIELD_TYPE, TYPE_ARGUMENT, RETURN_TYPE, PARAMETER_TYPE)
                    if (allMembers.contains(targetType) && isStructuralEdge(edge.kind())) {
                        internalEdges++;
                    }
                }
            }
        }

        // Calculate cohesion: ratio of actual connections to maximum possible
        // Maximum possible edges in a connected graph = n * (n-1) for directed graph
        // But for aggregates, a reasonable expectation is root connects to all, so minimum = n-1
        // We use a more generous formula: actual / expected where expected = n-1
        int expectedMinimumEdges = memberCount - 1;
        double cohesion = Math.min(1.0, (double) internalEdges / expectedMinimumEdges);

        return Optional.of(Math.round(cohesion * 100.0) / 100.0); // Round to 2 decimals
    }

    private boolean isStructuralEdge(EdgeKind kind) {
        return kind == EdgeKind.FIELD_TYPE
                || kind == EdgeKind.TYPE_ARGUMENT
                || kind == EdgeKind.RETURN_TYPE
                || kind == EdgeKind.PARAMETER_TYPE
                || kind == EdgeKind.USES_AS_COLLECTION_ELEMENT;
    }

    @Override
    public Optional<String> findRepositoryForAggregate(String aggregateRootType) {
        if (aggregateRootType == null) {
            return Optional.empty();
        }

        // Find a repository port that manages this aggregate
        for (Port port : ports) {
            if (port.isRepository()) {
                // Check if this repository manages the aggregate
                if (aggregateRootType.equals(port.primaryManagedType())) {
                    return Optional.of(port.qualifiedName());
                }
                // Also check managed types list
                if (port.managedTypes() != null && port.managedTypes().contains(aggregateRootType)) {
                    return Optional.of(port.qualifiedName());
                }
            }
        }

        // Fallback: try to match by naming convention
        String simpleAggregateName = extractSimpleName(aggregateRootType);
        for (Port port : ports) {
            if (port.isRepository()) {
                String simplePortName = port.simpleName();
                // Check if repository name contains aggregate name (e.g., OrderRepository for Order)
                if (simplePortName.startsWith(simpleAggregateName)
                        || simplePortName.contains(simpleAggregateName)) {
                    return Optional.of(port.qualifiedName());
                }
            }
        }

        return Optional.empty();
    }

    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    private String extractQualifiedNameFromNodeId(NodeId nodeId) {
        // NodeId format is "type:qualified.name", extract the part after ":"
        String value = nodeId.value();
        int colonIndex = value.indexOf(':');
        return colonIndex >= 0 ? value.substring(colonIndex + 1) : value;
    }
}
