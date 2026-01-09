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

package io.hexaglue.core.graph.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

/**
 * Generic cycle detector using Tarjan's Strongly Connected Components algorithm.
 *
 * <p>This implementation is generic and works with any graph representation where:
 * <ul>
 *   <li>Nodes are of type {@code NODE}</li>
 *   <li>Edges are of type {@code EDGE}</li>
 *   <li>Edge sources and targets can be extracted via provided functions</li>
 * </ul>
 *
 * <p>The algorithm finds all strongly connected components (SCCs) in the graph.
 * An SCC with more than one node or a self-loop represents a cycle.
 *
 * <p>This is a deterministic algorithm - given the same graph, it will always
 * produce the same results in the same order.
 *
 * @param <NODE> the node type
 * @param <EDGE> the edge type
 * @since 3.0.0
 */
public final class TarjanCycleDetector<NODE, EDGE> {

    private final Function<EDGE, NODE> getSource;
    private final Function<EDGE, NODE> getTarget;

    // Algorithm state
    private int index;
    private final Stack<NODE> stack;
    private final Map<NODE, Integer> indices;
    private final Map<NODE, Integer> lowLinks;
    private final Set<NODE> onStack;
    private final List<List<NODE>> sccs;

    /**
     * Creates a new cycle detector.
     *
     * @param getSource function to extract source node from edge
     * @param getTarget function to extract target node from edge
     */
    public TarjanCycleDetector(Function<EDGE, NODE> getSource, Function<EDGE, NODE> getTarget) {
        this.getSource = Objects.requireNonNull(getSource, "getSource required");
        this.getTarget = Objects.requireNonNull(getTarget, "getTarget required");

        this.stack = new Stack<>();
        this.indices = new HashMap<>();
        this.lowLinks = new HashMap<>();
        this.onStack = new HashSet<>();
        this.sccs = new ArrayList<>();
        this.index = 0;
    }

    /**
     * Detects cycles in the given graph.
     *
     * <p>A cycle is represented as a list of edges forming the cycle.
     * The detector returns up to {@code maxCycles} cycles.
     *
     * @param nodes  all nodes in the graph
     * @param edges  all edges in the graph
     * @param config detection configuration
     * @return list of detected cycles
     */
    public List<Cycle<EDGE>> detectCycles(Set<NODE> nodes, List<EDGE> edges, CycleDetectionConfig config) {
        Objects.requireNonNull(nodes, "nodes required");
        Objects.requireNonNull(edges, "edges required");
        Objects.requireNonNull(config, "config required");

        // Build adjacency list
        Map<NODE, List<EDGE>> adjacency = buildAdjacencyList(edges);

        // Reset state
        index = 0;
        stack.clear();
        indices.clear();
        lowLinks.clear();
        onStack.clear();
        sccs.clear();

        // Run Tarjan's algorithm on all nodes
        for (NODE node : nodes) {
            if (!indices.containsKey(node)) {
                strongConnect(node, adjacency);
            }
        }

        // Convert SCCs to cycles
        List<Cycle<EDGE>> cycles = new ArrayList<>();
        for (List<NODE> scc : sccs) {
            if (scc.size() > 1 || hasSelfLoop(scc.get(0), edges)) {
                // This is a cycle
                List<EDGE> cycleEdges = extractCycleEdges(scc, edges, config);
                if (!cycleEdges.isEmpty()) {
                    cycles.add(new Cycle<>(cycleEdges));
                }
            }

            // Stop if we've reached max cycles
            if (cycles.size() >= config.maxCycles()) {
                break;
            }
        }

        return cycles;
    }

    /**
     * Tarjan's strong connect algorithm.
     *
     * @param node      the current node
     * @param adjacency the adjacency list
     */
    private void strongConnect(NODE node, Map<NODE, List<EDGE>> adjacency) {
        // Set the depth index for this node
        indices.put(node, index);
        lowLinks.put(node, index);
        index++;

        stack.push(node);
        onStack.add(node);

        // Consider successors
        List<EDGE> outgoingEdges = adjacency.getOrDefault(node, List.of());
        for (EDGE edge : outgoingEdges) {
            NODE successor = getTarget.apply(edge);

            if (!indices.containsKey(successor)) {
                // Successor has not yet been visited; recurse
                strongConnect(successor, adjacency);
                lowLinks.put(node, Math.min(lowLinks.get(node), lowLinks.get(successor)));
            } else if (onStack.contains(successor)) {
                // Successor is on the stack, hence in the current SCC
                lowLinks.put(node, Math.min(lowLinks.get(node), indices.get(successor)));
            }
        }

        // If node is a root node, pop the stack to create an SCC
        if (lowLinks.get(node).equals(indices.get(node))) {
            List<NODE> scc = new ArrayList<>();
            NODE w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (!w.equals(node));

            sccs.add(scc);
        }
    }

    /**
     * Builds an adjacency list from edges.
     *
     * @param edges the edges
     * @return map from node to outgoing edges
     */
    private Map<NODE, List<EDGE>> buildAdjacencyList(List<EDGE> edges) {
        Map<NODE, List<EDGE>> adjacency = new HashMap<>();
        for (EDGE edge : edges) {
            NODE source = getSource.apply(edge);
            adjacency.computeIfAbsent(source, k -> new ArrayList<>()).add(edge);
        }
        return adjacency;
    }

    /**
     * Checks if a node has a self-loop.
     *
     * @param node  the node to check
     * @param edges all edges
     * @return true if the node has a self-loop
     */
    private boolean hasSelfLoop(NODE node, List<EDGE> edges) {
        for (EDGE edge : edges) {
            if (getSource.apply(edge).equals(node) && getTarget.apply(edge).equals(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts edges forming a cycle from an SCC.
     *
     * @param scc    the strongly connected component
     * @param edges  all edges in the graph
     * @param config detection configuration
     * @return list of edges forming the cycle
     */
    private List<EDGE> extractCycleEdges(List<NODE> scc, List<EDGE> edges, CycleDetectionConfig config) {
        Set<NODE> sccSet = new HashSet<>(scc);
        List<EDGE> cycleEdges = new ArrayList<>();

        // Find edges within the SCC
        for (EDGE edge : edges) {
            NODE source = getSource.apply(edge);
            NODE target = getTarget.apply(edge);

            if (sccSet.contains(source) && sccSet.contains(target)) {
                cycleEdges.add(edge);

                // Stop if we've reached max dependencies per edge
                if (cycleEdges.size() >= config.maxDependenciesPerEdge()) {
                    break;
                }
            }
        }

        return cycleEdges;
    }
}
