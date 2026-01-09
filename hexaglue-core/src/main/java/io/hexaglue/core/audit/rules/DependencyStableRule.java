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

import io.hexaglue.spi.audit.AuditRule;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.RuleViolation;
import io.hexaglue.spi.audit.Severity;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Audit rule that checks Stable Dependencies Principle (SDP).
 *
 * <p>The Stable Dependencies Principle states that dependencies should point
 * in the direction of stability. A module should only depend on modules that
 * are more stable than it is. Stability is measured by the instability metric:
 *
 * <pre>
 * I = Ce / (Ca + Ce)
 * </pre>
 *
 * <p>Where:
 * <ul>
 *   <li>Ce = Efferent coupling (outgoing dependencies)</li>
 *   <li>Ca = Afferent coupling (incoming dependencies)</li>
 *   <li>I = 0.0 means maximally stable (depended upon, but depends on nothing)</li>
 *   <li>I = 1.0 means maximally unstable (depends on others, nothing depends on it)</li>
 * </ul>
 *
 * <p>A violation occurs when a MORE stable component (lower I) depends on a
 * LESS stable component (higher I). This creates rigid architectures because
 * changes to the unstable component require changes to the stable one.
 *
 * <p><b>Context Requirement:</b> This rule requires access to the full codebase
 * dependency graph via {@link #setCodebaseContext(Codebase)}. The context must
 * be set before checking units, typically by the audit infrastructure.
 *
 * <p><b>Algorithm:</b> For each code unit, calculates its instability and the
 * instability of its dependencies. Reports violations where I(unit) &lt; I(dependency).
 *
 * @since 3.0.0
 */
public final class DependencyStableRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.dependency.stable";
    private static final String RULE_NAME = "Stable Dependencies Principle";

    /**
     * ThreadLocal context to provide codebase-level dependency information.
     * This is necessary because the AuditRule interface only receives individual
     * CodeUnits, but stability calculation requires knowledge of the entire dependency graph.
     */
    private static final ThreadLocal<Codebase> CODEBASE_CONTEXT = new ThreadLocal<>();

    /**
     * Sets the codebase context for stability checking.
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
        return Severity.WARNING;
    }

    @Override
    public List<RuleViolation> check(CodeUnit unit) {
        Codebase codebase = CODEBASE_CONTEXT.get();
        if (codebase == null) {
            // No context available - cannot perform stability checking
            return List.of();
        }

        Map<String, Set<String>> dependencies = codebase.dependencies();
        if (dependencies.isEmpty()) {
            return List.of();
        }

        // Build stability cache for all units
        Map<String, Double> stabilityCache = new HashMap<>();
        for (String unitName : dependencies.keySet()) {
            stabilityCache.put(unitName, calculateInstability(unitName, dependencies));
        }

        // Check stability violations for this unit's dependencies
        List<RuleViolation> violations = new ArrayList<>();
        String unitName = unit.qualifiedName();
        double unitInstability = stabilityCache.getOrDefault(unitName, 0.0);

        Set<String> unitDependencies = dependencies.getOrDefault(unitName, Set.of());
        for (String dependency : unitDependencies) {
            double dependencyInstability = stabilityCache.getOrDefault(dependency, 0.0);

            // Violation: more stable unit (lower I) depends on less stable unit (higher I)
            if (unitInstability < dependencyInstability) {
                violations.add(createViolation(unit, dependency, unitInstability, dependencyInstability));
            }
        }

        return violations;
    }

    /**
     * Calculates the instability metric for a code unit.
     *
     * <p>Instability I = Ce / (Ca + Ce), where:
     * <ul>
     *   <li>Ce = Efferent coupling (outgoing dependencies from this unit)</li>
     *   <li>Ca = Afferent coupling (incoming dependencies to this unit)</li>
     * </ul>
     *
     * <p>If Ca + Ce = 0 (isolated component), returns 0.0 (considered stable).
     *
     * @param unitName the qualified name of the unit
     * @param dependencies the full dependency graph
     * @return instability value between 0.0 (stable) and 1.0 (unstable)
     */
    private double calculateInstability(String unitName, Map<String, Set<String>> dependencies) {
        // Ce = number of outgoing dependencies
        int ce = dependencies.getOrDefault(unitName, Set.of()).size();

        // Ca = number of incoming dependencies (how many units depend on this one)
        int ca = 0;
        for (Set<String> deps : dependencies.values()) {
            if (deps.contains(unitName)) {
                ca++;
            }
        }

        int total = ca + ce;
        return total == 0 ? 0.0 : (double) ce / total;
    }

    /**
     * Creates a rule violation for a stability principle violation.
     *
     * @param unit the code unit that violates the principle
     * @param dependency the dependency that is less stable
     * @param unitInstability the instability of the unit
     * @param dependencyInstability the instability of the dependency
     * @return the rule violation
     */
    private RuleViolation createViolation(
            CodeUnit unit, String dependency, double unitInstability, double dependencyInstability) {
        String message = String.format(
                "Stable Dependencies Principle violation: '%s' (I=%.2f) depends on less stable '%s' (I=%.2f). "
                        + "Stable components should not depend on unstable ones. "
                        + "Consider inverting the dependency by introducing an interface in '%s' that '%s' implements, "
                        + "or restructure to make '%s' more stable by reducing its dependencies.",
                unit.qualifiedName(),
                unitInstability,
                dependency,
                dependencyInstability,
                unit.qualifiedName(),
                dependency,
                dependency);

        // Use a synthetic location since stability violations are structural issues
        SourceLocation location = SourceLocation.of(unit.qualifiedName(), 1, 1);

        return RuleViolation.warning(RULE_ID, message, location);
    }
}
