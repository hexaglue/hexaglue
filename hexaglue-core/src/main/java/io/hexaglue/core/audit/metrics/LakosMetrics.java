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

package io.hexaglue.core.audit.metrics;

/**
 * Lakos metrics for assessing large-scale architectural quality.
 *
 * <p>John Lakos introduced these metrics in "Large-Scale C++ Software Design"
 * to measure the maintainability and testability of large codebases. They are
 * language-agnostic and apply equally well to Java systems.
 *
 * <p>Metrics:
 * <ul>
 *   <li><b>CCD (Cumulative Component Dependency)</b>: Sum of transitive dependencies
 *       for all components. Lower is better.</li>
 *   <li><b>ACD (Average Component Dependency)</b>: CCD / number of components.
 *       Indicates average dependency burden.</li>
 *   <li><b>NCCD (Normalized CCD)</b>: CCD compared to a balanced binary tree.
 *       Values close to 1.0 are ideal.</li>
 *   <li><b>RACD (Relative ACD)</b>: ACD / theoretical minimum ACD. Values close
 *       to 1.0 indicate optimal dependency structure.</li>
 * </ul>
 *
 * @param componentCount number of components (packages or types)
 * @param ccd            cumulative component dependency
 * @param acd            average component dependency
 * @param nccd           normalized CCD
 * @param racd           relative ACD
 * @since 3.0.0
 */
public record LakosMetrics(int componentCount, int ccd, double acd, double nccd, double racd) {

    /**
     * Returns the quality level based on Lakos metrics.
     *
     * @return the quality level
     */
    public QualityLevel qualityLevel() {
        // NCCD interpretation (lower is better):
        // < 1.5: Excellent
        // 1.5 - 2.0: Good
        // 2.0 - 3.0: Acceptable
        // 3.0 - 5.0: Warning
        // > 5.0: Critical

        if (nccd < 1.5) {
            return QualityLevel.EXCELLENT;
        } else if (nccd < 2.0) {
            return QualityLevel.GOOD;
        } else if (nccd < 3.0) {
            return QualityLevel.ACCEPTABLE;
        } else if (nccd < 5.0) {
            return QualityLevel.WARNING;
        } else {
            return QualityLevel.CRITICAL;
        }
    }

    /**
     * Returns a human-readable assessment of the metrics.
     *
     * @return assessment message
     */
    public String assessment() {
        return switch (qualityLevel()) {
            case EXCELLENT -> "Excellent dependency structure. Components are well-organized with minimal coupling.";
            case GOOD -> "Good dependency structure. Minor improvements possible.";
            case ACCEPTABLE -> "Acceptable dependency structure. Consider refactoring high-dependency components.";
            case WARNING -> "Dependency structure needs attention. High coupling may impact maintainability.";
            case CRITICAL -> "Critical dependency issues. Significant refactoring recommended.";
        };
    }

    /**
     * Quality level enumeration.
     */
    public enum QualityLevel {
        /** Excellent quality (NCCD < 1.5) */
        EXCELLENT,

        /** Good quality (NCCD 1.5-2.0) */
        GOOD,

        /** Acceptable quality (NCCD 2.0-3.0) */
        ACCEPTABLE,

        /** Warning level (NCCD 3.0-5.0) */
        WARNING,

        /** Critical issues (NCCD > 5.0) */
        CRITICAL
    }
}
