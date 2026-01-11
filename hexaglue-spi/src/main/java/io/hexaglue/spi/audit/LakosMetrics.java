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

package io.hexaglue.spi.audit;

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
 * <p><b>Note:</b> This record contains only derived calculations (pure functions).
 * The actual collection of CCD is done in the Core's LakosMetricsCalculator.
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
     * Creates an empty metrics instance (for error cases or empty codebases).
     *
     * @return empty metrics with all values at 0
     */
    public static LakosMetrics empty() {
        return new LakosMetrics(0, 0, 0.0, 0.0, 0.0);
    }

    /**
     * Returns the quality level based on NCCD.
     *
     * <p>This is a derived calculation - a pure function of nccd.
     *
     * @return the quality level
     */
    public QualityLevel qualityLevel() {
        return QualityLevel.fromNCCD(nccd);
    }

    /**
     * Returns a human-readable assessment of the metrics.
     *
     * @return assessment message
     */
    public String assessment() {
        return qualityLevel().assessment();
    }

    /**
     * Returns true if the metrics indicate problems requiring attention.
     *
     * @return true if qualityLevel is WARNING or CRITICAL
     */
    public boolean requiresAttention() {
        return qualityLevel().requiresAttention();
    }

    /**
     * Returns true if this represents an empty or invalid metrics set.
     *
     * @return true if componentCount is 0
     */
    public boolean isEmpty() {
        return componentCount == 0;
    }
}
