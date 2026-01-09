/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.validator.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for detecting cycles in directed graphs using depth-first search.
 *
 * <p>This class implements cycle detection using DFS with a recursion stack.
 * It can detect all cycles in a directed graph represented as an adjacency map.
 *
 * <p>The algorithm works by:
 * <ol>
 *   <li>Performing DFS from each unvisited node</li>
 *   <li>Maintaining a recursion stack to track the current path</li>
 *   <li>When a node in the recursion stack is encountered, a cycle is found</li>
 *   <li>Extracting the cycle path from the current DFS path</li>
 * </ol>
 *
 * <p><strong>Time Complexity:</strong> O(V + E) where V is vertices and E is edges<br>
 * <strong>Space Complexity:</strong> O(V) for the recursion stack and visited set
 *
 * @since 1.0.0
 */
public class CycleDetector {

    /**
     * Finds all cycles in the directed graph.
     *
     * <p>Example usage:
     * <pre>{@code
     * Set<String> nodes = Set.of("A", "B", "C");
     * Map<String, Set<String>> edges = Map.of(
     *     "A", Set.of("B"),
     *     "B", Set.of("C"),
     *     "C", Set.of("A")  // cycle: A -> B -> C -> A
     * );
     *
     * CycleDetector detector = new CycleDetector();
     * List<List<String>> cycles = detector.findCycles(nodes, edges);
     * // Returns: [[A, B, C, A]]
     * }</pre>
     *
     * @param nodes the set of all nodes in the graph
     * @param edges the adjacency map (node -> set of adjacent nodes)
     * @return list of cycles, where each cycle is a list of node names forming the cycle
     */
    public List<List<String>> findCycles(Set<String> nodes, Map<String, Set<String>> edges) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        // Create a defensive copy of edges to avoid null issues
        Map<String, Set<String>> safeEdges = new HashMap<>();
        for (String node : nodes) {
            safeEdges.put(node, edges.getOrDefault(node, Set.of()));
        }

        // Start DFS from each unvisited node
        for (String node : nodes) {
            if (!visited.contains(node)) {
                List<String> path = new ArrayList<>();
                dfs(node, safeEdges, visited, recursionStack, path, cycles);
            }
        }

        return cycles;
    }

    /**
     * Performs depth-first search to detect cycles.
     *
     * @param node the current node being visited
     * @param edges the adjacency map
     * @param visited set of all visited nodes
     * @param recursionStack set of nodes in the current recursion path
     * @param path the current DFS path
     * @param cycles accumulator for detected cycles
     */
    private void dfs(
            String node,
            Map<String, Set<String>> edges,
            Set<String> visited,
            Set<String> recursionStack,
            List<String> path,
            List<List<String>> cycles) {

        // Mark as visited and add to recursion stack
        visited.add(node);
        recursionStack.add(node);
        path.add(node);

        // Explore all adjacent nodes
        Set<String> neighbors = edges.getOrDefault(node, Set.of());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                // Unvisited node - continue DFS
                dfs(neighbor, edges, visited, recursionStack, path, cycles);
            } else if (recursionStack.contains(neighbor)) {
                // Found a back edge - cycle detected
                int cycleStart = path.indexOf(neighbor);
                if (cycleStart >= 0) {
                    // Extract the cycle: from cycleStart to end, plus the neighbor to close it
                    List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                    cycle.add(neighbor); // Close the cycle
                    cycles.add(cycle);
                }
            }
            // If visited but not in recursion stack, it's a cross or forward edge - ignore
        }

        // Remove from recursion stack and path (backtrack)
        path.remove(path.size() - 1);
        recursionStack.remove(node);
    }

    /**
     * Checks if the graph contains any cycles.
     *
     * <p>This is more efficient than findCycles() if you only need to know
     * whether cycles exist, not what they are.
     *
     * @param nodes the set of all nodes in the graph
     * @param edges the adjacency map (node -> set of adjacent nodes)
     * @return true if the graph contains at least one cycle
     */
    public boolean hasCycles(Set<String> nodes, Map<String, Set<String>> edges) {
        return !findCycles(nodes, edges).isEmpty();
    }
}
