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

import io.hexaglue.plugin.audit.adapter.report.model.HealthScore;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CouplingMetrics;
import io.hexaglue.spi.audit.DependencyCycle;
import io.hexaglue.spi.audit.LakosMetrics;
import java.util.List;
import java.util.Objects;

/**
 * Calculates the overall health score of the codebase.
 *
 * <p>The health score is a composite metric computed from:
 * <ul>
 *   <li>DDD Compliance (25%): Adherence to Domain-Driven Design patterns</li>
 *   <li>Hexagonal Compliance (25%): Adherence to Hexagonal Architecture principles</li>
 *   <li>Dependency Quality (20%): Absence of cycles and stability violations</li>
 *   <li>Coupling (15%): Low coupling between packages</li>
 *   <li>Cohesion (15%): High cohesion within packages</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class HealthScoreCalculator {

    private static final int CYCLE_PENALTY = 5;
    private static final int STABILITY_VIOLATION_PENALTY = 3;

    private final ComplianceCalculator complianceCalculator;

    /**
     * Creates a new HealthScoreCalculator with a ComplianceCalculator.
     *
     * @param complianceCalculator the compliance calculator to use
     */
    public HealthScoreCalculator(ComplianceCalculator complianceCalculator) {
        this.complianceCalculator = Objects.requireNonNull(complianceCalculator, "complianceCalculator required");
    }

    /**
     * Creates a new HealthScoreCalculator with a default ComplianceCalculator.
     */
    public HealthScoreCalculator() {
        this(new ComplianceCalculator());
    }

    /**
     * Calculates the health score for the codebase.
     *
     * @param violations        the list of violations found
     * @param architectureQuery the architecture query interface (may be null)
     * @return the computed health score
     */
    public HealthScore calculate(List<Violation> violations, ArchitectureQuery architectureQuery) {
        Objects.requireNonNull(violations, "violations required");

        int dddCompliance = complianceCalculator.calculateDddCompliance(violations);
        int hexCompliance = complianceCalculator.calculateHexCompliance(violations);
        int dependencyQuality = calculateDependencyQuality(architectureQuery);
        int coupling = calculateCouplingScore(architectureQuery);
        int cohesion = calculateCohesionScore(architectureQuery);

        return HealthScore.compute(dddCompliance, hexCompliance, dependencyQuality, coupling, cohesion);
    }

    /**
     * Calculates the dependency quality score (0-100).
     *
     * <p>Deducts points for dependency cycles and stability violations.
     *
     * @param architectureQuery the architecture query interface
     * @return dependency quality score
     */
    private int calculateDependencyQuality(ArchitectureQuery architectureQuery) {
        if (architectureQuery == null) {
            return 100; // No data available, assume perfect
        }

        int score = 100;

        // Penalize for dependency cycles
        List<DependencyCycle> cycles = architectureQuery.findDependencyCycles();
        score -= cycles.size() * CYCLE_PENALTY;

        // Penalize for stability violations
        int stabilityViolations = architectureQuery.findStabilityViolations().size();
        score -= stabilityViolations * STABILITY_VIOLATION_PENALTY;

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Calculates the coupling score (0-100, higher is better = lower coupling).
     *
     * <p>Uses the average instability metric from package coupling analysis.
     * Lower instability in core packages indicates better design.
     *
     * @param architectureQuery the architecture query interface
     * @return coupling quality score
     */
    private int calculateCouplingScore(ArchitectureQuery architectureQuery) {
        if (architectureQuery == null) {
            return 100; // No data available, assume perfect
        }

        List<CouplingMetrics> allCoupling = architectureQuery.analyzeAllPackageCoupling();
        if (allCoupling.isEmpty()) {
            return 100;
        }

        // Calculate average distance from main sequence
        double avgDistance = allCoupling.stream()
                .mapToDouble(CouplingMetrics::distanceFromMainSequence)
                .average()
                .orElse(0.0);

        // Convert distance (0-1) to a score (100-0)
        // Distance = 0 means on main sequence (good), Distance = 1 means far from it (bad)
        return (int) Math.round((1.0 - avgDistance) * 100);
    }

    /**
     * Calculates the cohesion score (0-100, higher is better = higher cohesion).
     *
     * <p>Uses Lakos NCCD metric for cohesion measurement.
     * Lower NCCD indicates better modular design.
     *
     * @param architectureQuery the architecture query interface
     * @return cohesion quality score
     */
    private int calculateCohesionScore(ArchitectureQuery architectureQuery) {
        if (architectureQuery == null) {
            return 100; // No data available, assume perfect
        }

        LakosMetrics globalMetrics = architectureQuery.calculateGlobalLakosMetrics();
        if (globalMetrics == null) {
            return 100;
        }

        double nccd = globalMetrics.nccd();

        // NCCD typically ranges from 0 to 1+ (can exceed 1 for poor designs)
        // Score = 100 for NCCD=0 (perfect), decreasing as NCCD increases
        // Cap at NCCD=2 for score calculation purposes
        double normalizedNccd = Math.min(nccd, 2.0) / 2.0;

        return (int) Math.round((1.0 - normalizedNccd) * 100);
    }
}
