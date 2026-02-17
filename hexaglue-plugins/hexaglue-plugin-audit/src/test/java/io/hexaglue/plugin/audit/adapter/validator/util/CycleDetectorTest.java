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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CycleDetector}.
 *
 * <p>Validates the graph cycle detection algorithm using DFS with recursion stack.
 */
class CycleDetectorTest {

    private CycleDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CycleDetector();
    }

    @Test
    @DisplayName("Should find direct cycle")
    void shouldFindDirectCycle() {
        // Given: A -> B -> A
        Set<String> nodes = Set.of("A", "B");
        Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "B", Set.of("A"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).hasSize(1);
        assertThat(cycles.get(0)).hasSize(3); // Cycle: A -> B -> A (or B -> A -> B)
        // Cycle can start from any node
        assertThat(cycles.get(0)).contains("A", "B");
    }

    @Test
    @DisplayName("Should find indirect cycle")
    void shouldFindIndirectCycle() {
        // Given: A -> B -> C -> A
        Set<String> nodes = Set.of("A", "B", "C");
        Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "B", Set.of("C"), "C", Set.of("A"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).hasSize(1);
        assertThat(cycles.get(0)).hasSize(4); // Cycle detected
        // Cycle can start from any node (A->B->C->A or C->A->B->C, etc.)
        assertThat(cycles.get(0)).contains("A", "B", "C");
    }

    @Test
    @DisplayName("Should return empty when no cycles")
    void shouldReturnEmptyWhenNoCycles() {
        // Given: A -> B -> C (linear)
        Set<String> nodes = Set.of("A", "B", "C");
        Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "B", Set.of("C"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).isEmpty();
    }

    @Test
    @DisplayName("Should handle disconnected graph")
    void shouldHandleDisconnectedGraph() {
        // Given: A -> B (disconnected from C -> D)
        Set<String> nodes = Set.of("A", "B", "C", "D");
        Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "C", Set.of("D"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).isEmpty();
    }

    @Test
    @DisplayName("Should detect self-cycle")
    void shouldDetectSelfCycle() {
        // Given: A -> A
        Set<String> nodes = Set.of("A");
        Map<String, Set<String>> edges = Map.of("A", Set.of("A"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).hasSize(1);
        assertThat(cycles.get(0)).containsSequence("A", "A");
    }

    @Test
    @DisplayName("Should detect multiple cycles")
    void shouldDetectMultipleCycles() {
        // Given: A -> B -> A and C -> D -> C
        Set<String> nodes = Set.of("A", "B", "C", "D");
        Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "B", Set.of("A"), "C", Set.of("D"), "D", Set.of("C"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should handle empty graph")
    void shouldHandleEmptyGraph() {
        // Given: Empty graph
        Set<String> nodes = Set.of();
        Map<String, Set<String>> edges = Map.of();

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).isEmpty();
    }

    @Test
    @DisplayName("Should handle single node with no edges")
    void shouldHandleSingleNodeWithNoEdges() {
        // Given: A (no edges)
        Set<String> nodes = Set.of("A");
        Map<String, Set<String>> edges = Map.of();

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).isEmpty();
    }

    @Test
    @DisplayName("Should handle complex graph with cycle")
    void shouldHandleComplexGraphWithCycle() {
        // Given: A -> B -> C -> D -> B (cycle) and E -> F (no cycle)
        Set<String> nodes = Set.of("A", "B", "C", "D", "E", "F");
        Map<String, Set<String>> edges = new HashMap<>();
        edges.put("A", Set.of("B"));
        edges.put("B", Set.of("C"));
        edges.put("C", Set.of("D"));
        edges.put("D", Set.of("B")); // Back to B, creating cycle
        edges.put("E", Set.of("F"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).hasSize(1);
        assertThat(cycles.get(0)).contains("B", "C", "D");
    }

    @Test
    @DisplayName("Should handle diamond structure without cycle")
    void shouldHandleDiamondStructureWithoutCycle() {
        // Given: A -> B -> D, A -> C -> D (diamond, no cycle)
        Set<String> nodes = Set.of("A", "B", "C", "D");
        Map<String, Set<String>> edges = Map.of("A", Set.of("B", "C"), "B", Set.of("D"), "C", Set.of("D"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then
        assertThat(cycles).isEmpty();
    }

    @Test
    @DisplayName("hasCycles should return true when cycles exist")
    void hasCycles_shouldReturnTrue_whenCyclesExist() {
        // Given: A -> B -> A
        Set<String> nodes = Set.of("A", "B");
        Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "B", Set.of("A"));

        // When
        boolean result = detector.hasCycles(nodes, edges);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasCycles should return false when no cycles")
    void hasCycles_shouldReturnFalse_whenNoCycles() {
        // Given: A -> B -> C
        Set<String> nodes = Set.of("A", "B", "C");
        Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "B", Set.of("C"));

        // When
        boolean result = detector.hasCycles(nodes, edges);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle nodes with null edge sets")
    void shouldHandleNodesWithNullEdgeSets() {
        // Given: Nodes exist but some have no edges defined
        Set<String> nodes = Set.of("A", "B", "C");
        Map<String, Set<String>> edges = new HashMap<>();
        edges.put("A", Set.of("B"));
        // B and C have no edges defined

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then: Should not throw and should return empty
        assertThat(cycles).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple edges between same nodes")
    void shouldHandleMultipleEdgesBetweenSameNodes() {
        // Given: A -> B with multiple logical connections (represented as one edge in Set)
        Set<String> nodes = Set.of("A", "B");
        Set<String> aEdges = new HashSet<>();
        aEdges.add("B");
        aEdges.add("B"); // Duplicate, but Set will deduplicate
        Map<String, Set<String>> edges = Map.of("A", aEdges, "B", Set.of("A"));

        // When
        List<List<String>> cycles = detector.findCycles(nodes, edges);

        // Then: Should still detect the cycle
        assertThat(cycles).hasSize(1);
    }

    @Nested
    @DisplayName("When finding strongly connected components")
    class WhenFindingStronglyConnectedComponents {

        @Test
        @DisplayName("Should return empty for acyclic graph")
        void shouldReturnEmpty_forAcyclicGraph() {
            // Given: A → B → C (no cycle)
            Set<String> nodes = Set.of("A", "B", "C");
            Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "B", Set.of("C"));

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then
            assertThat(sccs).isEmpty();
        }

        @Test
        @DisplayName("Should find simple cycle as single SCC")
        void shouldFindSimpleCycle_asSingleScc() {
            // Given: A → B → C → A
            Set<String> nodes = Set.of("A", "B", "C");
            Map<String, Set<String>> edges = Map.of(
                    "A", Set.of("B"),
                    "B", Set.of("C"),
                    "C", Set.of("A"));

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then
            assertThat(sccs).hasSize(1);
            assertThat(sccs.get(0)).containsExactlyInAnyOrder("A", "B", "C");
        }

        @Test
        @DisplayName("Should find mutual dependency as SCC")
        void shouldFindMutualDependency_asScc() {
            // Given: A ↔ B
            Set<String> nodes = Set.of("A", "B");
            Map<String, Set<String>> edges = Map.of(
                    "A", Set.of("B"),
                    "B", Set.of("A"));

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then
            assertThat(sccs).hasSize(1);
            assertThat(sccs.get(0)).containsExactlyInAnyOrder("A", "B");
        }

        @Test
        @DisplayName("Should find two disjoint SCCs")
        void shouldFindTwoDisjointSccs() {
            // Given: A ↔ B and C → D → E → C
            Set<String> nodes = Set.of("A", "B", "C", "D", "E");
            Map<String, Set<String>> edges = Map.of(
                    "A", Set.of("B"),
                    "B", Set.of("A"),
                    "C", Set.of("D"),
                    "D", Set.of("E"),
                    "E", Set.of("C"));

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then
            assertThat(sccs).hasSize(2);
            List<Set<String>> sorted = sccs.stream()
                    .sorted((a, b) -> Integer.compare(a.size(), b.size()))
                    .toList();
            assertThat(sorted.get(0)).containsExactlyInAnyOrder("A", "B");
            assertThat(sorted.get(1)).containsExactlyInAnyOrder("C", "D", "E");
        }

        @Test
        @DisplayName("Should exclude self-loops (size 1)")
        void shouldExcludeSelfLoops() {
            // Given: A → A (self-loop)
            Set<String> nodes = Set.of("A");
            Map<String, Set<String>> edges = Map.of("A", Set.of("A"));

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then: self-loops are excluded (SCC of size 1)
            assertThat(sccs).isEmpty();
        }

        @Test
        @DisplayName("Should merge overlapping SCCs into single component")
        void shouldMergeOverlappingSccs() {
            // Given: A → B → C → A and B → D → E → B
            // Both cycles share B, so all nodes form a single SCC
            Set<String> nodes = Set.of("A", "B", "C", "D", "E");
            Map<String, Set<String>> edges = Map.of(
                    "A", Set.of("B"),
                    "B", Set.of("C", "D"),
                    "C", Set.of("A"),
                    "D", Set.of("E"),
                    "E", Set.of("B"));

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then: all nodes in one SCC
            assertThat(sccs).hasSize(1);
            assertThat(sccs.get(0)).containsExactlyInAnyOrder("A", "B", "C", "D", "E");
        }

        @Test
        @DisplayName("Should return empty for empty graph")
        void shouldReturnEmpty_forEmptyGraph() {
            // Given
            Set<String> nodes = Set.of();
            Map<String, Set<String>> edges = Map.of();

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then
            assertThat(sccs).isEmpty();
        }

        @Test
        @DisplayName("Should return immutable sets")
        void shouldReturnImmutableSets() {
            // Given: A ↔ B
            Set<String> nodes = Set.of("A", "B");
            Map<String, Set<String>> edges = Map.of(
                    "A", Set.of("B"),
                    "B", Set.of("A"));

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then
            assertThat(sccs).hasSize(1);
            assertThatThrownBy(() -> sccs.get(0).add("Z")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should handle 10000-node chain without stack overflow")
        void shouldHandle10000NodeChain_withoutStackOverflow() {
            // Given: linear chain of 10000 nodes (no cycle)
            int size = 10_000;
            Set<String> nodes = IntStream.range(0, size).mapToObj(i -> "N" + i).collect(Collectors.toSet());
            Map<String, Set<String>> edges = new HashMap<>();
            for (int i = 0; i < size - 1; i++) {
                edges.put("N" + i, Set.of("N" + (i + 1)));
            }

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then: no stack overflow, no SCCs
            assertThat(sccs).isEmpty();
        }

        @Test
        @DisplayName("Should handle 5000-node cycle without stack overflow")
        void shouldHandle5000NodeCycle_withoutStackOverflow() {
            // Given: cycle of 5000 nodes
            int size = 5_000;
            Set<String> nodes = IntStream.range(0, size).mapToObj(i -> "N" + i).collect(Collectors.toSet());
            Map<String, Set<String>> edges = new HashMap<>();
            for (int i = 0; i < size; i++) {
                edges.put("N" + i, Set.of("N" + ((i + 1) % size)));
            }

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then: single SCC of size 5000
            assertThat(sccs).hasSize(1);
            assertThat(sccs.get(0)).hasSize(size);
        }

        @Test
        @DisplayName("Should find SCC even with external connections")
        void shouldFindScc_evenWithExternalConnections() {
            // Given: X → A → B → C → A → Y
            Set<String> nodes = Set.of("X", "A", "B", "C", "Y");
            Map<String, Set<String>> edges = Map.of(
                    "X", Set.of("A"),
                    "A", Set.of("B", "Y"),
                    "B", Set.of("C"),
                    "C", Set.of("A"));

            // When
            List<Set<String>> sccs = detector.findStronglyConnectedComponents(nodes, edges);

            // Then: only {A, B, C} is an SCC
            assertThat(sccs).hasSize(1);
            assertThat(sccs.get(0)).containsExactlyInAnyOrder("A", "B", "C");
        }
    }

    @Nested
    @DisplayName("When computing SCC mapping")
    class WhenComputingSccMapping {

        @Test
        @DisplayName("Should map each node to itself for acyclic graph")
        void shouldMapEachNodeToItself_forAcyclicGraph() {
            // Given: A → B → C (no cycle)
            Set<String> nodes = Set.of("A", "B", "C");
            Map<String, Set<String>> edges = Map.of("A", Set.of("B"), "B", Set.of("C"));

            // When
            Map<String, String> mapping = detector.computeSccMapping(nodes, edges);

            // Then: each node maps to itself
            assertThat(mapping).hasSize(3);
            assertThat(mapping.get("A")).isEqualTo("A");
            assertThat(mapping.get("B")).isEqualTo("B");
            assertThat(mapping.get("C")).isEqualTo("C");
        }

        @Test
        @DisplayName("Should map all cycle members to same representative")
        void shouldMapAllCycleMembers_toSameRepresentative() {
            // Given: A → B → C → A
            Set<String> nodes = Set.of("A", "B", "C");
            Map<String, Set<String>> edges = Map.of(
                    "A", Set.of("B"),
                    "B", Set.of("C"),
                    "C", Set.of("A"));

            // When
            Map<String, String> mapping = detector.computeSccMapping(nodes, edges);

            // Then: all members have same representative
            assertThat(mapping).hasSize(3);
            String representative = mapping.get("A");
            assertThat(mapping.get("B")).isEqualTo(representative);
            assertThat(mapping.get("C")).isEqualTo(representative);
            // Representative must be one of the members
            assertThat(representative).isIn("A", "B", "C");
        }

        @Test
        @DisplayName("Should cover all nodes in mapping")
        void shouldCoverAllNodes_inMapping() {
            // Given: mixed graph with SCC and non-SCC nodes
            Set<String> nodes = Set.of("X", "A", "B", "C", "Y");
            Map<String, Set<String>> edges = new HashMap<>();
            edges.put("X", Set.of("A"));
            edges.put("A", Set.of("B", "Y"));
            edges.put("B", Set.of("C"));
            edges.put("C", Set.of("A"));

            // When
            Map<String, String> mapping = detector.computeSccMapping(nodes, edges);

            // Then: every node is present
            assertThat(mapping.keySet()).containsExactlyInAnyOrderElementsOf(nodes);
        }

        @Test
        @DisplayName("Should return immutable map")
        void shouldReturnImmutableMap() {
            // Given: A → B
            Set<String> nodes = Set.of("A", "B");
            Map<String, Set<String>> edges = Map.of("A", Set.of("B"));

            // When
            Map<String, String> mapping = detector.computeSccMapping(nodes, edges);

            // Then
            assertThatThrownBy(() -> mapping.put("Z", "Z")).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
