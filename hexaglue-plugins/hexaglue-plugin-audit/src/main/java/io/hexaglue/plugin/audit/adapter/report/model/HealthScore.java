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

package io.hexaglue.plugin.audit.adapter.report.model;

/**
 * Represents the overall health score of the audited codebase.
 *
 * <p>The health score is a composite metric (0-100) calculated from weighted components:
 * <ul>
 *   <li>DDD Compliance (25%): Adherence to Domain-Driven Design patterns</li>
 *   <li>Hexagonal Compliance (25%): Adherence to Hexagonal Architecture principles</li>
 *   <li>Dependency Quality (20%): Absence of cycles and stability violations</li>
 *   <li>Coupling (15%): Low coupling between packages</li>
 *   <li>Cohesion (15%): High cohesion within packages</li>
 * </ul>
 *
 * @param overall           the composite score (0-100)
 * @param dddCompliance     DDD compliance score (0-100)
 * @param hexCompliance     hexagonal architecture compliance score (0-100)
 * @param dependencyQuality dependency quality score (0-100)
 * @param coupling          coupling quality score (0-100, higher is better)
 * @param cohesion          cohesion quality score (0-100, higher is better)
 * @param grade             letter grade (A-F) based on overall score
 * @since 1.0.0
 */
public record HealthScore(
        int overall,
        int dddCompliance,
        int hexCompliance,
        int dependencyQuality,
        int coupling,
        int cohesion,
        String grade) {

    /** Weight for DDD compliance in overall score calculation. */
    public static final double DDD_WEIGHT = 0.25;

    /** Weight for hexagonal compliance in overall score calculation. */
    public static final double HEX_WEIGHT = 0.25;

    /** Weight for dependency quality in overall score calculation. */
    public static final double DEP_WEIGHT = 0.20;

    /** Weight for coupling quality in overall score calculation. */
    public static final double COUPLING_WEIGHT = 0.15;

    /** Weight for cohesion quality in overall score calculation. */
    public static final double COHESION_WEIGHT = 0.15;

    public HealthScore {
        validateScore("overall", overall);
        validateScore("dddCompliance", dddCompliance);
        validateScore("hexCompliance", hexCompliance);
        validateScore("dependencyQuality", dependencyQuality);
        validateScore("coupling", coupling);
        validateScore("cohesion", cohesion);
        if (grade == null || grade.isBlank()) {
            throw new IllegalArgumentException("grade cannot be null or blank");
        }
        if (!grade.matches("[A-F]")) {
            throw new IllegalArgumentException("grade must be A, B, C, D, E, or F");
        }
    }

    private static void validateScore(String name, int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(name + " must be between 0 and 100: " + value);
        }
    }

    /**
     * Computes a health score from individual component scores.
     *
     * @param ddd  DDD compliance score (0-100)
     * @param hex  hexagonal compliance score (0-100)
     * @param dep  dependency quality score (0-100)
     * @param coup coupling quality score (0-100)
     * @param coh  cohesion quality score (0-100)
     * @return a new HealthScore with calculated overall and grade
     */
    public static HealthScore compute(int ddd, int hex, int dep, int coup, int coh) {
        int overall = (int) (ddd * DDD_WEIGHT + hex * HEX_WEIGHT + dep * DEP_WEIGHT + coup * COUPLING_WEIGHT
                + coh * COHESION_WEIGHT);
        String grade = computeGrade(overall);
        return new HealthScore(overall, ddd, hex, dep, coup, coh, grade);
    }

    /**
     * Returns a zero health score (all components at 0).
     *
     * @return a HealthScore with all values at 0 and grade F
     */
    public static HealthScore zero() {
        return new HealthScore(0, 0, 0, 0, 0, 0, "F");
    }

    /**
     * Returns a perfect health score (all components at 100).
     *
     * @return a HealthScore with all values at 100 and grade A
     */
    public static HealthScore perfect() {
        return new HealthScore(100, 100, 100, 100, 100, 100, "A");
    }

    private static String computeGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
