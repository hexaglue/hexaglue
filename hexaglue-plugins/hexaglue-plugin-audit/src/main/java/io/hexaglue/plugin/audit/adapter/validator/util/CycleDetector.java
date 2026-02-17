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

package io.hexaglue.plugin.audit.adapter.validator.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

    /**
     * Finds all strongly connected components (SCCs) of size &ge; 2 in the directed graph.
     *
     * <p>Uses an iterative implementation of Tarjan's algorithm to avoid stack overflow
     * on large graphs. SCCs of size 1 (including self-loops) are excluded from the result
     * since they do not represent mutual dependencies between distinct nodes.
     *
     * <p>Example usage:
     * <pre>{@code
     * Set<String> nodes = Set.of("A", "B", "C", "D");
     * Map<String, Set<String>> edges = Map.of(
     *     "A", Set.of("B"), "B", Set.of("A"),  // SCC: {A, B}
     *     "C", Set.of("D")                      // no SCC
     * );
     *
     * CycleDetector detector = new CycleDetector();
     * List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);
     * // Returns: [{A, B}]
     * }</pre>
     *
     * @param nodes the set of all nodes in the graph
     * @param edges the adjacency map (node &rarr; set of adjacent nodes)
     * @return list of SCCs with size &ge; 2, each as an unmodifiable set
     * @since 5.1.0
     */
    public List<Set<String>> findStronglyConnectedComponents(Set<String> nodes, Map<String, Set<String>> edges) {
        List<Set<String>> allSccs = findAllSccs(nodes, edges);
        List<Set<String>> result = new ArrayList<>();
        for (Set<String> scc : allSccs) {
            if (scc.size() >= 2) {
                result.add(scc);
            }
        }
        return result;
    }

    /**
     * Computes a mapping from each node to its SCC representative.
     *
     * <p>Nodes that belong to the same SCC are mapped to the same representative string.
     * Nodes that are not part of any cycle map to themselves. The representative is
     * an arbitrary member of the SCC (determined by Tarjan's traversal order).
     *
     * <p>This mapping is useful for contracting SCCs into super-nodes when building
     * a condensed DAG (e.g., for computing longest dependency paths).
     *
     * @param nodes the set of all nodes in the graph
     * @param edges the adjacency map (node &rarr; set of adjacent nodes)
     * @return unmodifiable map from each node to its SCC representative
     * @since 5.1.0
     */
    public Map<String, String> computeSccMapping(Set<String> nodes, Map<String, Set<String>> edges) {
        List<Set<String>> allSccs = findAllSccs(nodes, edges);

        Map<String, String> mapping = new HashMap<>();
        for (Set<String> scc : allSccs) {
            String representative = scc.iterator().next();
            for (String member : scc) {
                mapping.put(member, representative);
            }
        }
        // Nodes not in any SCC from findAllSccs map to themselves
        for (String node : nodes) {
            mapping.putIfAbsent(node, node);
        }
        return Collections.unmodifiableMap(mapping);
    }

    /**
     * Finds all SCCs using iterative Tarjan's algorithm (including size-1 components).
     */
    private List<Set<String>> findAllSccs(Set<String> nodes, Map<String, Set<String>> edges) {
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowlink = new HashMap<>();
        Set<String> onStack = new HashSet<>();
        Deque<String> tarjanStack = new ArrayDeque<>();
        List<Set<String>> result = new ArrayList<>();
        int[] counter = {0};

        // Iterative call stack
        Deque<TarjanFrame> callStack = new ArrayDeque<>();

        for (String node : nodes) {
            if (!index.containsKey(node)) {
                // Push initial frame
                index.put(node, counter[0]);
                lowlink.put(node, counter[0]);
                counter[0]++;
                onStack.add(node);
                tarjanStack.push(node);

                Iterator<String> neighbors = edges.getOrDefault(node, Set.of()).iterator();
                callStack.push(new TarjanFrame(node, neighbors, null));

                while (!callStack.isEmpty()) {
                    TarjanFrame frame = callStack.peek();

                    if (frame.neighbors().hasNext()) {
                        String neighbor = frame.neighbors().next();

                        if (!index.containsKey(neighbor)) {
                            // Discover new node
                            index.put(neighbor, counter[0]);
                            lowlink.put(neighbor, counter[0]);
                            counter[0]++;
                            onStack.add(neighbor);
                            tarjanStack.push(neighbor);

                            Iterator<String> neighborEdges =
                                    edges.getOrDefault(neighbor, Set.of()).iterator();
                            callStack.push(new TarjanFrame(neighbor, neighborEdges, frame.node()));
                        } else if (onStack.contains(neighbor)) {
                            // Update lowlink for back edge
                            lowlink.put(frame.node(), Math.min(lowlink.get(frame.node()), index.get(neighbor)));
                        }
                    } else {
                        // All neighbors explored, check if this is an SCC root
                        if (lowlink.get(frame.node()).equals(index.get(frame.node()))) {
                            Set<String> scc = new HashSet<>();
                            String popped;
                            do {
                                popped = tarjanStack.pop();
                                onStack.remove(popped);
                                scc.add(popped);
                            } while (!popped.equals(frame.node()));
                            result.add(Collections.unmodifiableSet(scc));
                        }

                        // Propagate lowlink to parent
                        callStack.pop();
                        if (frame.parent() != null) {
                            lowlink.put(
                                    frame.parent(), Math.min(lowlink.get(frame.parent()), lowlink.get(frame.node())));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Stack frame for iterative Tarjan's algorithm, replacing recursive calls.
     */
    private record TarjanFrame(String node, Iterator<String> neighbors, String parent) {}
}
