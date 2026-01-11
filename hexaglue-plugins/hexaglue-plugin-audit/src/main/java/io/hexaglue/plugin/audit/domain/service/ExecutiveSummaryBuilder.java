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

package io.hexaglue.plugin.audit.domain.service;

import io.hexaglue.plugin.audit.adapter.report.model.ArchitectureAnalysis;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory;
import io.hexaglue.plugin.audit.adapter.report.model.ExecutiveSummary;
import io.hexaglue.plugin.audit.adapter.report.model.ExecutiveSummary.ConcernEntry;
import io.hexaglue.plugin.audit.adapter.report.model.ExecutiveSummary.KpiEntry;
import io.hexaglue.plugin.audit.adapter.report.model.HealthScore;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds the executive summary from audit results.
 *
 * <p>The executive summary provides a high-level overview for stakeholders
 * including verdict, key strengths, concerns, KPIs, and immediate actions.
 *
 * @since 1.0.0
 */
public class ExecutiveSummaryBuilder {

    /**
     * Builds an executive summary from the audit results.
     *
     * @param healthScore    the health score
     * @param inventory      the component inventory
     * @param violations     the list of violations
     * @param analysis       the architecture analysis
     * @param metrics        the collected metrics
     * @param dddCompliance  DDD compliance percentage
     * @param hexCompliance  hexagonal compliance percentage
     * @return the executive summary
     */
    public ExecutiveSummary build(
            HealthScore healthScore,
            ComponentInventory inventory,
            List<Violation> violations,
            ArchitectureAnalysis analysis,
            Map<String, Metric> metrics,
            int dddCompliance,
            int hexCompliance) {
        Objects.requireNonNull(healthScore, "healthScore required");
        Objects.requireNonNull(inventory, "inventory required");
        Objects.requireNonNull(violations, "violations required");

        String verdict = createVerdict(healthScore, violations);
        List<String> strengths = identifyStrengths(healthScore, inventory, analysis, dddCompliance, hexCompliance);
        List<ConcernEntry> concerns = identifyConcerns(violations, analysis);
        List<KpiEntry> kpis = buildKpis(healthScore, metrics, dddCompliance, hexCompliance);
        List<String> immediateActions = determineImmediateActions(violations, analysis);

        return new ExecutiveSummary(verdict, strengths, concerns, kpis, immediateActions);
    }

    /**
     * Creates the verdict text based on health score and violations.
     */
    private String createVerdict(HealthScore healthScore, List<Violation> violations) {
        int score = healthScore.overall();
        long blockers = violations.stream().filter(v -> v.severity() == Severity.BLOCKER).count();
        long criticals = violations.stream().filter(v -> v.severity() == Severity.CRITICAL).count();

        if (blockers > 0) {
            return "Architecture requires immediate attention. " + blockers
                    + " blocker issue(s) must be resolved before proceeding.";
        }

        if (criticals > 0) {
            return "Architecture has critical issues requiring prompt attention. "
                    + "Review and address " + criticals + " critical violation(s).";
        }

        if (score >= 90) {
            return "Excellent architectural health. The codebase demonstrates strong adherence to "
                    + "DDD and Hexagonal Architecture principles.";
        }

        if (score >= 80) {
            return "Good architectural health with room for improvement. "
                    + "Address identified violations to further strengthen the architecture.";
        }

        if (score >= 70) {
            return "Acceptable architectural health. Several areas need attention to improve "
                    + "compliance with architectural principles.";
        }

        if (score >= 60) {
            return "Architecture needs improvement. Significant violations detected that should be "
                    + "addressed to maintain code quality.";
        }

        return "Architecture requires significant restructuring. Multiple violations indicate "
                + "fundamental architectural issues that need resolution.";
    }

    /**
     * Identifies strengths based on audit results.
     */
    private List<String> identifyStrengths(
            HealthScore healthScore,
            ComponentInventory inventory,
            ArchitectureAnalysis analysis,
            int dddCompliance,
            int hexCompliance) {
        List<String> strengths = new ArrayList<>();

        // DDD compliance strength
        if (dddCompliance >= 90) {
            strengths.add("Strong DDD pattern adherence (" + dddCompliance + "%)");
        } else if (dddCompliance >= 80) {
            strengths.add("Good DDD pattern implementation (" + dddCompliance + "%)");
        }

        // Hexagonal compliance strength
        if (hexCompliance >= 90) {
            strengths.add("Excellent hexagonal architecture compliance (" + hexCompliance + "%)");
        } else if (hexCompliance >= 80) {
            strengths.add("Good hexagonal architecture structure (" + hexCompliance + "%)");
        }

        // Domain model strength
        if (inventory.aggregateRoots() > 0 && inventory.valueObjects() >= inventory.aggregateRoots()) {
            strengths.add("Well-structured domain model with proper value object usage");
        }

        // Architecture cleanliness
        if (analysis != null && analysis.isClean()) {
            strengths.add("No dependency cycles or layer violations detected");
        }

        // High cohesion
        if (healthScore.cohesion() >= 80) {
            strengths.add("High package cohesion (" + healthScore.cohesion() + "%)");
        }

        // Low coupling
        if (healthScore.coupling() >= 80) {
            strengths.add("Low inter-package coupling (" + healthScore.coupling() + "%)");
        }

        // Port coverage
        if (inventory.drivingPorts() > 0 && inventory.drivenPorts() > 0) {
            strengths.add("Clear port separation (driving: " + inventory.drivingPorts() + ", driven: "
                    + inventory.drivenPorts() + ")");
        }

        return strengths;
    }

    /**
     * Identifies concerns from violations and architecture analysis.
     */
    private List<ConcernEntry> identifyConcerns(List<Violation> violations, ArchitectureAnalysis analysis) {
        List<ConcernEntry> concerns = new ArrayList<>();

        // Group violations by severity
        Map<Severity, Long> bySeverity = violations.stream()
                .collect(Collectors.groupingBy(Violation::severity, Collectors.counting()));

        // Add concerns for each severity level
        long blockers = bySeverity.getOrDefault(Severity.BLOCKER, 0L);
        if (blockers > 0) {
            concerns.add(new ConcernEntry("BLOCKER", "Critical architectural violations requiring immediate action", (int) blockers));
        }

        long criticals = bySeverity.getOrDefault(Severity.CRITICAL, 0L);
        if (criticals > 0) {
            concerns.add(new ConcernEntry("CRITICAL", "Serious violations affecting architecture integrity", (int) criticals));
        }

        long majors = bySeverity.getOrDefault(Severity.MAJOR, 0L);
        if (majors > 0) {
            concerns.add(new ConcernEntry("MAJOR", "Significant violations impacting code quality", (int) majors));
        }

        // Architecture-specific concerns
        if (analysis != null) {
            int cycles = analysis.totalCycles();
            if (cycles > 0) {
                concerns.add(new ConcernEntry("ARCHITECTURE", "Dependency cycles detected", cycles));
            }

            if (!analysis.layerViolations().isEmpty()) {
                concerns.add(new ConcernEntry("LAYER", "Layer boundary violations", analysis.layerViolations().size()));
            }
        }

        return concerns;
    }

    /**
     * Builds KPI entries from health score and metrics.
     */
    private List<KpiEntry> buildKpis(
            HealthScore healthScore, Map<String, Metric> metrics, int dddCompliance, int hexCompliance) {
        List<KpiEntry> kpis = new ArrayList<>();

        // Overall health score
        kpis.add(new KpiEntry("Health Score", healthScore.overall() + "/100", ">=80", scoreStatus(healthScore.overall(), 80)));

        // DDD compliance
        kpis.add(new KpiEntry("DDD Compliance", dddCompliance + "%", ">=90%", scoreStatus(dddCompliance, 90)));

        // Hexagonal compliance
        kpis.add(new KpiEntry("Hexagonal Compliance", hexCompliance + "%", ">=90%", scoreStatus(hexCompliance, 90)));

        // Dependency quality
        kpis.add(new KpiEntry(
                "Dependency Quality",
                healthScore.dependencyQuality() + "%",
                ">=80%",
                scoreStatus(healthScore.dependencyQuality(), 80)));

        // Coupling
        kpis.add(new KpiEntry("Coupling Quality", healthScore.coupling() + "%", ">=70%", scoreStatus(healthScore.coupling(), 70)));

        // Cohesion
        kpis.add(new KpiEntry("Cohesion Quality", healthScore.cohesion() + "%", ">=70%", scoreStatus(healthScore.cohesion(), 70)));

        return kpis;
    }

    /**
     * Determines immediate actions based on violations and analysis.
     */
    private List<String> determineImmediateActions(List<Violation> violations, ArchitectureAnalysis analysis) {
        List<String> actions = new ArrayList<>();

        // Count blockers
        long blockers = violations.stream().filter(v -> v.severity() == Severity.BLOCKER).count();
        if (blockers > 0) {
            actions.add("Resolve " + blockers + " blocker violation(s) immediately");
        }

        // Count criticals
        long criticals = violations.stream().filter(v -> v.severity() == Severity.CRITICAL).count();
        if (criticals > 0) {
            actions.add("Address " + criticals + " critical violation(s) as priority");
        }

        // Dependency cycles
        if (analysis != null && analysis.totalCycles() > 0) {
            actions.add("Break " + analysis.totalCycles() + " dependency cycle(s) to improve modularity");
        }

        // Layer violations
        if (analysis != null && !analysis.layerViolations().isEmpty()) {
            actions.add("Fix " + analysis.layerViolations().size() + " layer violation(s) to restore architectural boundaries");
        }

        // If no immediate actions, provide general guidance
        if (actions.isEmpty()) {
            actions.add("Review major violations for potential quick wins");
            actions.add("Consider addressing minor issues during regular development");
        }

        return actions;
    }

    private String scoreStatus(int value, int threshold) {
        if (value >= threshold) {
            return "OK";
        } else if (value >= threshold - 10) {
            return "WARNING";
        }
        return "CRITICAL";
    }
}
