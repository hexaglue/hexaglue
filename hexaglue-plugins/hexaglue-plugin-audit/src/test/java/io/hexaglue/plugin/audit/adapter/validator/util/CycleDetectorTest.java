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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
}
