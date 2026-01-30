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

package io.hexaglue.core.graph.query;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.index.GraphIndexes;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.model.edges.*;
import io.hexaglue.core.graph.style.PackageOrganizationStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link GraphQuery}.
 */
public final class DefaultGraphQuery implements GraphQuery {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultGraphQuery.class);

    private final ApplicationGraph graph;
    private final GraphIndexes indexes;

    public DefaultGraphQuery(ApplicationGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
        this.indexes = graph.indexes();
    }

    // === Type queries ===

    @Override
    public Stream<TypeNode> types() {
        return graph.typeNodes().stream();
    }

    @Override
    public Stream<TypeNode> types(Predicate<TypeNode> predicate) {
        return types().filter(predicate);
    }

    @Override
    public Stream<TypeNode> typesInPackage(String packageName) {
        return indexes.typesByPackage(packageName).stream().map(graph::typeNode).flatMap(Optional::stream);
    }

    @Override
    public Stream<TypeNode> typesWithForm(JavaForm form) {
        return indexes.typesByForm(form).stream().map(graph::typeNode).flatMap(Optional::stream);
    }

    @Override
    public Stream<TypeNode> typesAnnotatedWith(String annotationQualifiedName) {
        return indexes.byAnnotation(annotationQualifiedName).stream()
                .filter(NodeId::isType)
                .map(graph::typeNode)
                .flatMap(Optional::stream);
    }

    // === Member queries ===

    @Override
    public Stream<FieldNode> fields() {
        return graph.memberNodes().stream().filter(m -> m instanceof FieldNode).map(m -> (FieldNode) m);
    }

    @Override
    public Stream<FieldNode> fields(Predicate<FieldNode> predicate) {
        return fields().filter(predicate);
    }

    @Override
    public Stream<MethodNode> methods() {
        return graph.memberNodes().stream().filter(m -> m instanceof MethodNode).map(m -> (MethodNode) m);
    }

    @Override
    public Stream<MethodNode> methods(Predicate<MethodNode> predicate) {
        return methods().filter(predicate);
    }

    @Override
    public Stream<ConstructorNode> constructors() {
        return graph.memberNodes().stream()
                .filter(m -> m instanceof ConstructorNode)
                .map(m -> (ConstructorNode) m);
    }

    // === Relationship queries ===

    @Override
    public List<FieldNode> fieldsOf(TypeNode type) {
        return graph.fieldsOf(type);
    }

    @Override
    public List<MethodNode> methodsOf(TypeNode type) {
        return graph.methodsOf(type);
    }

    @Override
    public List<ConstructorNode> constructorsOf(TypeNode type) {
        return indexes.membersOf(type.id()).stream()
                .map(id -> graph.node(id).orElse(null))
                .filter(n -> n instanceof ConstructorNode)
                .map(n -> (ConstructorNode) n)
                .toList();
    }

    @Override
    public Optional<TypeNode> supertypeOf(TypeNode type) {
        return graph.supertypeOf(type);
    }

    @Override
    public List<TypeNode> interfacesOf(TypeNode type) {
        return graph.interfacesOf(type);
    }

    @Override
    public List<TypeNode> subtypesOf(TypeNode type) {
        return indexes.subtypesOf(type.id()).stream()
                .map(graph::typeNode)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<TypeNode> implementorsOf(TypeNode interfaceType) {
        return indexes.implementorsOf(interfaceType.id()).stream()
                .map(graph::typeNode)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<TypeNode> usersInSignatureOf(TypeNode type) {
        return indexes.interfacesUsingInSignature(type.id()).stream()
                .map(graph::typeNode)
                .flatMap(Optional::stream)
                .toList();
    }

    // === Lookup queries ===

    @Override
    public Optional<TypeNode> type(String qualifiedName) {
        return graph.typeNode(qualifiedName);
    }

    @Override
    public Optional<TypeNode> type(NodeId id) {
        return graph.typeNode(id);
    }

    @Override
    public Optional<FieldNode> field(NodeId id) {
        return graph.fieldNode(id);
    }

    @Override
    public Optional<MethodNode> method(NodeId id) {
        return graph.methodNode(id);
    }

    // === Style queries ===

    @Override
    public PackageOrganizationStyle packageOrganizationStyle() {
        return graph.metadata().style();
    }

    @Override
    public ConfidenceLevel styleConfidence() {
        return graph.metadata().styleConfidence();
    }

    // === Typed edge queries ===

    @Override
    public List<MethodCallEdge> methodCallsFrom(NodeId nodeId) {
        return graph.edgesFrom(nodeId).stream()
                .filter(e -> isMethodCallEdge(e))
                .flatMap(e -> toMethodCallEdge(e).stream())
                .toList();
    }

    @Override
    public List<MethodCallEdge> methodCallsTo(NodeId nodeId) {
        return graph.edgesTo(nodeId).stream()
                .filter(e -> isMethodCallEdge(e))
                .flatMap(e -> toMethodCallEdge(e).stream())
                .toList();
    }

    @Override
    public List<FieldAccessEdge> fieldAccessesFrom(NodeId nodeId) {
        return graph.edgesFrom(nodeId).stream()
                .filter(e -> isFieldAccessEdge(e))
                .flatMap(e -> toFieldAccessEdge(e).stream())
                .toList();
    }

    @Override
    public List<FieldAccessEdge> fieldAccessesTo(NodeId nodeId) {
        return graph.edgesTo(nodeId).stream()
                .filter(e -> isFieldAccessEdge(e))
                .flatMap(e -> toFieldAccessEdge(e).stream())
                .toList();
    }

    // === Transitive queries ===

    @Override
    public List<TypeNode> transitiveDependencies(TypeNode type, int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1 (got " + maxDepth + ")");
        }

        Set<NodeId> visited = new LinkedHashSet<>();
        Queue<DepthNode> queue = new ArrayDeque<>();
        queue.add(new DepthNode(type.id(), 0));
        visited.add(type.id());

        while (!queue.isEmpty()) {
            DepthNode current = queue.poll();

            if (current.depth >= maxDepth) {
                continue;
            }

            // Get all outgoing edges (dependencies)
            for (Edge edge : graph.edgesFrom(current.nodeId)) {
                if (isDependencyEdge(edge) && !visited.contains(edge.to())) {
                    visited.add(edge.to());
                    queue.add(new DepthNode(edge.to(), current.depth + 1));
                }
            }
        }

        // Remove the starting type and convert to TypeNodes
        visited.remove(type.id());
        return visited.stream().map(graph::typeNode).flatMap(Optional::stream).toList();
    }

    @Override
    public List<TypeNode> transitiveDependents(TypeNode type, int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1 (got " + maxDepth + ")");
        }

        Set<NodeId> visited = new LinkedHashSet<>();
        Queue<DepthNode> queue = new ArrayDeque<>();
        queue.add(new DepthNode(type.id(), 0));
        visited.add(type.id());

        while (!queue.isEmpty()) {
            DepthNode current = queue.poll();

            if (current.depth >= maxDepth) {
                continue;
            }

            // Get all incoming edges (dependents)
            for (Edge edge : graph.edgesTo(current.nodeId)) {
                if (isDependencyEdge(edge) && !visited.contains(edge.from())) {
                    visited.add(edge.from());
                    queue.add(new DepthNode(edge.from(), current.depth + 1));
                }
            }
        }

        // Remove the starting type and convert to TypeNodes
        visited.remove(type.id());
        return visited.stream().map(graph::typeNode).flatMap(Optional::stream).toList();
    }

    @Override
    public boolean hasCyclicDependency(TypeNode type) {
        return hasCyclicDependencyDFS(type.id(), new HashSet<>(), new HashSet<>());
    }

    // === Path queries ===

    @Override
    public Optional<List<TypeNode>> shortestPath(TypeNode from, TypeNode to) {
        if (from.equals(to)) {
            return Optional.of(List.of(from));
        }

        Queue<List<NodeId>> queue = new ArrayDeque<>();
        Set<NodeId> visited = new HashSet<>();

        queue.add(List.of(from.id()));
        visited.add(from.id());

        while (!queue.isEmpty()) {
            List<NodeId> path = queue.poll();
            NodeId current = path.get(path.size() - 1);

            // Explore neighbors (outgoing edges)
            for (Edge edge : graph.edgesFrom(current)) {
                if (!isDependencyEdge(edge)) {
                    continue;
                }

                NodeId next = edge.to();

                if (next.equals(to.id())) {
                    // Found the target - build the complete path
                    List<NodeId> completePath = new ArrayList<>(path);
                    completePath.add(next);
                    return Optional.of(convertToTypeNodes(completePath));
                }

                if (!visited.contains(next)) {
                    visited.add(next);
                    List<NodeId> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }

        return Optional.empty();
    }

    // === Graph access ===

    @Override
    public ApplicationGraph graph() {
        return graph;
    }

    // === Helper methods ===

    /**
     * Checks if an edge represents a method call.
     */
    private boolean isMethodCallEdge(Edge edge) {
        return edge.isDerived()
                && edge.proof() != null
                && "METHOD_CALL".equals(edge.proof().derivationRule());
    }

    /**
     * Checks if an edge represents a field access.
     */
    private boolean isFieldAccessEdge(Edge edge) {
        return edge.isDerived()
                && edge.proof() != null
                && "FIELD_ACCESS".equals(edge.proof().derivationRule());
    }

    /**
     * Checks if an edge represents a dependency relationship.
     */
    private boolean isDependencyEdge(Edge edge) {
        return edge.kind() == EdgeKind.EXTENDS
                || edge.kind() == EdgeKind.IMPLEMENTS
                || edge.kind() == EdgeKind.FIELD_TYPE
                || edge.kind() == EdgeKind.PARAMETER_TYPE
                || edge.kind() == EdgeKind.RETURN_TYPE
                || edge.kind() == EdgeKind.TYPE_ARGUMENT
                || edge.kind() == EdgeKind.REFERENCES;
    }

    /**
     * Converts a basic Edge to a MethodCallEdge.
     *
     * <p>Since EdgeProof doesn't store full metadata, we create a simplified
     * MethodCallEdge using the edge's from/to nodes as caller/callee.
     *
     * @return the method call edge, or empty if conversion fails
     */
    private Optional<MethodCallEdge> toMethodCallEdge(Edge edge) {
        if (edge.proof() == null) {
            return Optional.empty();
        }

        try {
            // Parse basic info from the via field if possible
            String via = edge.proof().via();
            boolean isStaticCall = via != null && via.contains("static=true");
            boolean isConstructorCall = via != null && via.contains("ctor=true");

            // Use from/to as calling/called method (simplified representation)
            return Optional.of(new MethodCallEdge(
                    edge.from(),
                    edge.to(),
                    edge.from(), // callingMethod
                    edge.to(), // calledMethod
                    1, // invocationCount (default)
                    isStaticCall,
                    isConstructorCall));
        } catch (Exception e) {
            // Failed to create edge - log and return empty
            LOG.debug("Failed to convert edge to MethodCallEdge: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Converts a basic Edge to a FieldAccessEdge.
     *
     * <p>Since EdgeProof doesn't store full metadata, we parse the access type
     * from the via field if possible, defaulting to READ.
     *
     * @return the field access edge, or empty if conversion fails
     */
    private Optional<FieldAccessEdge> toFieldAccessEdge(Edge edge) {
        if (edge.proof() == null) {
            return Optional.empty();
        }

        try {
            // Parse access type from the via field if possible
            String via = edge.proof().via();
            FieldAccessEdge.AccessType accessType = FieldAccessEdge.AccessType.READ; // default

            if (via != null) {
                if (via.contains("type=WRITE")) {
                    accessType = FieldAccessEdge.AccessType.WRITE;
                } else if (via.contains("type=READ_WRITE")) {
                    accessType = FieldAccessEdge.AccessType.READ_WRITE;
                }
            }

            return Optional.of(new FieldAccessEdge(edge.from(), edge.to(), accessType));
        } catch (Exception e) {
            // Failed to create edge - log and return empty
            LOG.debug("Failed to convert edge to FieldAccessEdge: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * DFS helper for cycle detection.
     */
    private boolean hasCyclicDependencyDFS(NodeId current, Set<NodeId> visited, Set<NodeId> recursionStack) {
        visited.add(current);
        recursionStack.add(current);

        for (Edge edge : graph.edgesFrom(current)) {
            if (!isDependencyEdge(edge)) {
                continue;
            }

            NodeId neighbor = edge.to();

            if (!visited.contains(neighbor)) {
                if (hasCyclicDependencyDFS(neighbor, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                // Found a back edge (cycle)
                return true;
            }
        }

        recursionStack.remove(current);
        return false;
    }

    /**
     * Converts a list of NodeIds to TypeNodes.
     */
    private List<TypeNode> convertToTypeNodes(List<NodeId> nodeIds) {
        return nodeIds.stream().map(graph::typeNode).flatMap(Optional::stream).toList();
    }

    /**
     * Helper record for BFS traversal with depth tracking.
     */
    private record DepthNode(NodeId nodeId, int depth) {}
}
