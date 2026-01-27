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

package io.hexaglue.arch.model.audit;

/**
 * Quality level based on Lakos NCCD metric.
 *
 * <p>NCCD (Normalized Cumulative Component Dependency) measures the overall
 * coupling of a system compared to an ideal balanced tree structure.
 *
 * <p>Thresholds:
 * <ul>
 *   <li>EXCELLENT: NCCD &lt; 1.5</li>
 *   <li>GOOD: NCCD 1.5 - 2.0</li>
 *   <li>ACCEPTABLE: NCCD 2.0 - 3.0</li>
 *   <li>WARNING: NCCD 3.0 - 5.0</li>
 *   <li>CRITICAL: NCCD &gt; 5.0</li>
 * </ul>
 *
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public enum QualityLevel {

    /** Excellent quality (NCCD &lt; 1.5). Well-organized with minimal coupling. */
    EXCELLENT("Excellent dependency structure. Components are well-organized with minimal coupling."),

    /** Good quality (NCCD 1.5-2.0). Minor improvements possible. */
    GOOD("Good dependency structure. Minor improvements possible."),

    /** Acceptable quality (NCCD 2.0-3.0). Consider refactoring. */
    ACCEPTABLE("Acceptable dependency structure. Consider refactoring high-dependency components."),

    /** Warning level (NCCD 3.0-5.0). Needs attention. */
    WARNING("Dependency structure needs attention. High coupling may impact maintainability."),

    /** Critical issues (NCCD &gt; 5.0). Significant refactoring recommended. */
    CRITICAL("Critical dependency issues. Significant refactoring recommended.");

    private final String assessment;

    QualityLevel(String assessment) {
        this.assessment = assessment;
    }

    /**
     * Returns a human-readable assessment message.
     *
     * @return assessment message
     */
    public String assessment() {
        return assessment;
    }

    /**
     * Determines quality level from NCCD value.
     *
     * <p>Algorithm:
     * <pre>
     * if nccd &lt; 1.5  -&gt; EXCELLENT
     * if nccd &lt; 2.0  -&gt; GOOD
     * if nccd &lt; 3.0  -&gt; ACCEPTABLE
     * if nccd &lt; 5.0  -&gt; WARNING
     * else           -&gt; CRITICAL
     * </pre>
     *
     * @param nccd the normalized cumulative component dependency
     * @return the quality level
     */
    public static QualityLevel fromNCCD(double nccd) {
        if (nccd < 1.5) {
            return EXCELLENT;
        }
        if (nccd < 2.0) {
            return GOOD;
        }
        if (nccd < 3.0) {
            return ACCEPTABLE;
        }
        if (nccd < 5.0) {
            return WARNING;
        }
        return CRITICAL;
    }

    /**
     * Returns true if this level indicates problems requiring attention.
     *
     * @return true for WARNING or CRITICAL
     */
    public boolean requiresAttention() {
        return this == WARNING || this == CRITICAL;
    }
}
