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

package io.hexaglue.core.graph.composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A graph representing composition and reference relationships between domain types.
 *
 * <p>The composition graph is the foundation for deterministic domain classification.
 * It captures how domain types relate to each other through fields, enabling analysis of:
 * <ul>
 *   <li>Aggregate boundaries (composition relationships)</li>
 *   <li>Cross-aggregate references (reference-by-id relationships)</li>
 *   <li>Design smells (direct references between aggregates)</li>
 *   <li>Composition cycles</li>
 *   <li>Shared entities across aggregates</li>
 * </ul>
 *
 * <p>This class is immutable after construction.
 *
 * @since 3.0.0
 */
public final class CompositionGraph {

    private final Map<String, CompositionNode> nodes;
    private final List<CompositionEdge> edges;

    // Cached indices for efficient queries
    private final Map<String, Set<String>> composedTypesCache;
    private final Map<String, Set<String>> composingTypesCache;
    private final Map<String, Set<String>> referencedByIdTypesCache;
    private final Map<String, List<CompositionEdge>> outgoingEdgesCache;
    private final Map<String, List<CompositionEdge>> incomingEdgesCache;

    /**
     * Creates a composition graph with the given nodes and edges.
     *
     * @param nodes the nodes indexed by qualified name
     * @param edges the edges between nodes
     * @throws NullPointerException if nodes or edges is null
     */
    public CompositionGraph(Map<String, CompositionNode> nodes, List<CompositionEdge> edges) {
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(nodes, "nodes required")));
        this.edges = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(edges, "edges required")));

        // Build caches for efficient queries
        this.composedTypesCache = buildComposedTypesCache();
        this.composingTypesCache = buildComposingTypesCache();
        this.referencedByIdTypesCache = buildReferencedByIdTypesCache();
        this.outgoingEdgesCache = buildOutgoingEdgesCache();
        this.incomingEdgesCache = buildIncomingEdgesCache();
    }

    /**
     * Returns all nodes in the graph.
     *
     * @return unmodifiable map of nodes by qualified name
     */
    public Map<String, CompositionNode> getNodes() {
        return nodes;
    }

    /**
     * Returns all edges in the graph.
     *
     * @return unmodifiable list of edges
     */
    public List<CompositionEdge> getEdges() {
        return edges;
    }

    /**
     * Returns the node for the given qualified name.
     *
     * @param qualifiedName the type's qualified name
     * @return optional containing the node, or empty if not found
     */
    public Optional<CompositionNode> getNode(String qualifiedName) {
        return Optional.ofNullable(nodes.get(qualifiedName));
    }

    /**
     * Returns true if the graph contains a node for the given type.
     *
     * @param qualifiedName the type's qualified name
     * @return true if the type is in the graph
     */
    public boolean containsNode(String qualifiedName) {
        return nodes.containsKey(qualifiedName);
    }

    /**
     * Returns all types that are composed by (embedded in) the given type.
     *
     * <p>This includes only COMPOSITION relationships, not references.
     *
     * @param typeName the qualified name of the container type
     * @return set of qualified names of composed types (empty if none)
     */
    public Set<String> getComposedTypes(String typeName) {
        return composedTypesCache.getOrDefault(typeName, Collections.emptySet());
    }

    /**
     * Returns all types that compose (contain) the given type.
     *
     * <p>This is the inverse of {@link #getComposedTypes(String)}.
     *
     * @param typeName the qualified name of the contained type
     * @return set of qualified names of composing types (empty if none)
     */
    public Set<String> getComposingTypes(String typeName) {
        return composingTypesCache.getOrDefault(typeName, Collections.emptySet());
    }

    /**
     * Returns all types that are referenced by ID from the given type.
     *
     * <p>This includes only REFERENCE_BY_ID relationships.
     *
     * @param typeName the qualified name of the referencing type
     * @return set of qualified names of referenced types (empty if none)
     */
    public Set<String> getReferencedByIdTypes(String typeName) {
        return referencedByIdTypesCache.getOrDefault(typeName, Collections.emptySet());
    }

    /**
     * Returns all outgoing edges from the given type.
     *
     * @param typeName the qualified name of the source type
     * @return list of outgoing edges (empty if none)
     */
    public List<CompositionEdge> getOutgoingEdges(String typeName) {
        return outgoingEdgesCache.getOrDefault(typeName, Collections.emptyList());
    }

    /**
     * Returns all incoming edges to the given type.
     *
     * @param typeName the qualified name of the target type
     * @return list of incoming edges (empty if none)
     */
    public List<CompositionEdge> getIncomingEdges(String typeName) {
        return incomingEdgesCache.getOrDefault(typeName, Collections.emptyList());
    }

    /**
     * Returns true if the given type is a composition root.
     *
     * <p>A type is a composition root if it is not composed by any other type
     * (i.e., no incoming COMPOSITION edges). This typically indicates an aggregate root.
     *
     * @param typeName the qualified name of the type to check
     * @return true if the type has no incoming composition edges
     */
    public boolean isCompositionRoot(String typeName) {
        return getIncomingEdges(typeName).stream().noneMatch(CompositionEdge::isComposition);
    }

    /**
     * Returns all composition roots in the graph.
     *
     * <p>These are potential aggregate roots.
     *
     * @return set of qualified names of composition roots
     */
    public Set<String> getCompositionRoots() {
        return nodes.keySet().stream().filter(this::isCompositionRoot).collect(Collectors.toSet());
    }

    /**
     * Returns all direct reference edges (design smells).
     *
     * @return list of edges with DIRECT_REFERENCE type
     */
    public List<CompositionEdge> getDirectReferenceEdges() {
        return edges.stream().filter(CompositionEdge::isDirectReference).collect(Collectors.toList());
    }

    /**
     * Returns the transitive closure of composed types starting from the given type.
     *
     * <p>This computes all types reachable via COMPOSITION edges, representing
     * the full aggregate boundary.
     *
     * @param typeName the root type
     * @return set of all transitively composed types (including the root)
     */
    public Set<String> getTransitiveComposedTypes(String typeName) {
        Set<String> result = new HashSet<>();
        collectTransitiveComposedTypes(typeName, result);
        return result;
    }

    private void collectTransitiveComposedTypes(String typeName, Set<String> visited) {
        if (!visited.add(typeName)) {
            return; // Already visited (cycle detection)
        }
        for (String composed : getComposedTypes(typeName)) {
            collectTransitiveComposedTypes(composed, visited);
        }
    }

    /**
     * Exports the graph in GraphViz DOT format for visualization.
     *
     * <p>This is useful for debugging and understanding the domain structure.
     *
     * @return DOT format string
     */
    public String toDotFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CompositionGraph {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=box, style=rounded];\n\n");

        // Nodes
        for (CompositionNode node : nodes.values()) {
            String color = node.isIdWrapper() ? "lightblue" : node.hasIdentity() ? "lightgreen" : "lightyellow";
            String shape = node.isRecord() ? "component" : "box";
            sb.append(String.format(
                    "  \"%s\" [label=\"%s\", fillcolor=%s, shape=%s, style=\"rounded,filled\"];\n",
                    node.qualifiedName(), node.simpleName(), color, shape));
        }

        sb.append("\n");

        // Edges
        for (CompositionEdge edge : edges) {
            String style =
                    switch (edge.type()) {
                        case COMPOSITION -> "solid";
                        case REFERENCE_BY_ID -> "dashed";
                        case DIRECT_REFERENCE -> "dotted";
                    };
            String color = edge.type() == RelationType.DIRECT_REFERENCE ? "red" : "black";
            String label = edge.fieldName() + (edge.cardinality() == Cardinality.MANY ? "[]" : "");

            sb.append(String.format(
                    "  \"%s\" -> \"%s\" [label=\"%s\", style=%s, color=%s];\n",
                    edge.source(), edge.target(), label, style, color));
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Returns a summary of graph statistics.
     *
     * @return human-readable summary
     */
    public String getStats() {
        long compositionEdges =
                edges.stream().filter(CompositionEdge::isComposition).count();
        long referenceByIdEdges =
                edges.stream().filter(CompositionEdge::isReferenceById).count();
        long directReferenceEdges =
                edges.stream().filter(CompositionEdge::isDirectReference).count();
        long rootCount = getCompositionRoots().size();

        return String.format(
                "CompositionGraph: %d nodes, %d edges (%d composition, %d ref-by-id, %d direct-ref), %d roots",
                nodes.size(), edges.size(), compositionEdges, referenceByIdEdges, directReferenceEdges, rootCount);
    }

    // Cache builders

    private Map<String, Set<String>> buildComposedTypesCache() {
        Map<String, Set<String>> cache = new HashMap<>();
        for (CompositionEdge edge : edges) {
            if (edge.isComposition()) {
                cache.computeIfAbsent(edge.source(), k -> new HashSet<>()).add(edge.target());
            }
        }
        // Make immutable
        cache.replaceAll((k, v) -> Collections.unmodifiableSet(v));
        return cache;
    }

    private Map<String, Set<String>> buildComposingTypesCache() {
        Map<String, Set<String>> cache = new HashMap<>();
        for (CompositionEdge edge : edges) {
            if (edge.isComposition()) {
                cache.computeIfAbsent(edge.target(), k -> new HashSet<>()).add(edge.source());
            }
        }
        cache.replaceAll((k, v) -> Collections.unmodifiableSet(v));
        return cache;
    }

    private Map<String, Set<String>> buildReferencedByIdTypesCache() {
        Map<String, Set<String>> cache = new HashMap<>();
        for (CompositionEdge edge : edges) {
            if (edge.isReferenceById()) {
                cache.computeIfAbsent(edge.source(), k -> new HashSet<>()).add(edge.target());
            }
        }
        cache.replaceAll((k, v) -> Collections.unmodifiableSet(v));
        return cache;
    }

    private Map<String, List<CompositionEdge>> buildOutgoingEdgesCache() {
        Map<String, List<CompositionEdge>> cache = new HashMap<>();
        for (CompositionEdge edge : edges) {
            cache.computeIfAbsent(edge.source(), k -> new ArrayList<>()).add(edge);
        }
        cache.replaceAll((k, v) -> Collections.unmodifiableList(v));
        return cache;
    }

    private Map<String, List<CompositionEdge>> buildIncomingEdgesCache() {
        Map<String, List<CompositionEdge>> cache = new HashMap<>();
        for (CompositionEdge edge : edges) {
            cache.computeIfAbsent(edge.target(), k -> new ArrayList<>()).add(edge);
        }
        cache.replaceAll((k, v) -> Collections.unmodifiableList(v));
        return cache;
    }

    @Override
    public String toString() {
        return getStats();
    }
}
