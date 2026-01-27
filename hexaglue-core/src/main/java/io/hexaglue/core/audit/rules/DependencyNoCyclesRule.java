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

package io.hexaglue.core.audit.rules;

import io.hexaglue.arch.model.audit.CodeUnit;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.RuleViolation;
import io.hexaglue.arch.model.audit.Severity;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.spi.audit.AuditRule;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Audit rule that detects cyclic dependencies between types.
 *
 * <p>Cyclic dependencies create tight coupling and make code harder to test,
 * maintain, and evolve. This rule identifies cycles and suggests breaking them
 * through dependency inversion or restructuring.
 *
 * <p>This rule uses a depth-first search algorithm to detect cycles in the
 * dependency graph. When a cycle is detected involving the checked code unit,
 * a violation is reported with the complete cycle path.
 *
 * <p><b>Context Requirement:</b> This rule requires access to the full codebase
 * dependency graph via {@link #setCodebaseContext(Codebase)}. The context must
 * be set before checking units, typically by the audit infrastructure.
 *
 * <p><b>Algorithm:</b> Uses a modified DFS with a recursion stack to detect
 * back edges, which indicate cycles. This is similar to cycle detection in
 * directed graphs and runs in O(V + E) time where V is the number of code units
 * and E is the number of dependencies.
 *
 * @since 3.0.0
 */
public final class DependencyNoCyclesRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.dependency.no-cycles";
    private static final String RULE_NAME = "No Cyclic Dependencies";

    /**
     * ThreadLocal context to provide codebase-level dependency information.
     * This is necessary because the AuditRule interface only receives individual
     * CodeUnits, but cycle detection requires knowledge of the entire dependency graph.
     */
    private static final ThreadLocal<Codebase> CODEBASE_CONTEXT = new ThreadLocal<>();

    /**
     * Sets the codebase context for cycle detection.
     *
     * <p>This method must be called by the audit infrastructure before checking
     * code units. The context is stored in a ThreadLocal to maintain thread safety
     * while providing access to the full dependency graph.
     *
     * @param codebase the codebase containing all units and their dependencies
     */
    public static void setCodebaseContext(Codebase codebase) {
        CODEBASE_CONTEXT.set(codebase);
    }

    /**
     * Clears the codebase context.
     *
     * <p>Should be called after audit processing completes to prevent memory leaks
     * in thread pools.
     */
    public static void clearCodebaseContext() {
        CODEBASE_CONTEXT.remove();
    }

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public String name() {
        return RULE_NAME;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MAJOR;
    }

    @Override
    public List<RuleViolation> check(CodeUnit unit) {
        Codebase codebase = CODEBASE_CONTEXT.get();
        if (codebase == null) {
            // No context available - cannot perform cycle detection
            return List.of();
        }

        Map<String, Set<String>> dependencies = codebase.dependencies();
        if (dependencies.isEmpty()) {
            return List.of();
        }

        // Find all cycles in the dependency graph
        List<List<String>> cycles = findAllCycles(dependencies);

        // Filter to cycles that involve this unit
        List<RuleViolation> violations = new ArrayList<>();
        String unitName = unit.qualifiedName();

        for (List<String> cycle : cycles) {
            if (cycle.contains(unitName)) {
                violations.add(createViolation(unit, cycle));
            }
        }

        return violations;
    }

    /**
     * Finds all cycles in the dependency graph using depth-first search.
     *
     * @param dependencies the dependency map (source -> targets)
     * @return list of cycles, where each cycle is a list of qualified type names
     */
    private List<List<String>> findAllCycles(Map<String, Set<String>> dependencies) {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String type : dependencies.keySet()) {
            if (!visited.contains(type)) {
                findCyclesHelper(type, dependencies, visited, recursionStack, new ArrayDeque<>(), cycles);
            }
        }

        return cycles;
    }

    /**
     * Recursive helper for cycle detection using DFS.
     *
     * <p>The algorithm maintains a recursion stack to detect back edges. When we
     * encounter a node that's already in the recursion stack, we've found a cycle.
     *
     * @param current the current node being visited
     * @param dependencies the full dependency graph
     * @param visited set of all visited nodes
     * @param recursionStack set of nodes in the current DFS path
     * @param path the current path being explored
     * @param cycles output list to collect detected cycles
     */
    private void findCyclesHelper(
            String current,
            Map<String, Set<String>> dependencies,
            Set<String> visited,
            Set<String> recursionStack,
            Deque<String> path,
            List<List<String>> cycles) {

        visited.add(current);
        recursionStack.add(current);
        path.addLast(current);

        Set<String> deps = dependencies.getOrDefault(current, Set.of());

        for (String dependency : deps) {
            if (!recursionStack.contains(dependency)) {
                // Not yet visited in this path - continue DFS
                if (!visited.contains(dependency)) {
                    findCyclesHelper(dependency, dependencies, visited, recursionStack, path, cycles);
                }
            } else {
                // Found a back edge - this indicates a cycle
                Optional<List<String>> cyclePath = extractCyclePath(path, dependency);
                if (cyclePath.isPresent() && !isDuplicateCycle(cycles, cyclePath.get())) {
                    cycles.add(cyclePath.get());
                }
            }
        }

        path.removeLast();
        recursionStack.remove(current);
    }

    /**
     * Extracts the cycle path from the current DFS path.
     *
     * @param path the current DFS traversal path
     * @param cycleStart the node where the cycle begins
     * @return the cycle path if found, empty otherwise
     */
    private Optional<List<String>> extractCyclePath(Deque<String> path, String cycleStart) {
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
            return Optional.of(cyclePath);
        }

        return Optional.empty();
    }

    /**
     * Checks if a cycle is a duplicate of one already found.
     *
     * <p>Cycles are considered duplicates if they contain the same nodes,
     * regardless of the starting point or direction.
     *
     * @param existingCycles the cycles already detected
     * @param newCycle the new cycle to check
     * @return true if this cycle is a duplicate
     */
    private boolean isDuplicateCycle(List<List<String>> existingCycles, List<String> newCycle) {
        Set<String> newCycleSet = new HashSet<>(newCycle);
        for (List<String> existing : existingCycles) {
            Set<String> existingSet = new HashSet<>(existing);
            if (existingSet.equals(newCycleSet)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a rule violation for a detected cycle.
     *
     * @param unit the code unit involved in the cycle
     * @param cycle the cycle path
     * @return the rule violation
     */
    private RuleViolation createViolation(CodeUnit unit, List<String> cycle) {
        String cyclePath = String.join(" -> ", cycle);
        String message = String.format(
                "Cyclic dependency detected: %s. "
                        + "Consider breaking the cycle using dependency inversion (introduce an interface), "
                        + "merging the classes if they are too tightly coupled, or restructuring the design.",
                cyclePath);

        // Use a synthetic location since cycles are structural issues
        SourceLocation location = SourceLocation.of(unit.qualifiedName(), 1, 1);

        return RuleViolation.warning(RULE_ID, message, location);
    }
}
