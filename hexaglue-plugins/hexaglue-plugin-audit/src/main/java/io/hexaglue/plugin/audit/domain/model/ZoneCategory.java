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

package io.hexaglue.plugin.audit.domain.model;

/**
 * Categorizes packages according to Robert C. Martin's metrics.
 *
 * <p>This categorization is based on a package's position relative to the
 * <strong>Main Sequence</strong> in the Abstractness-Instability plane.
 * The Main Sequence represents the ideal balance between abstractness and
 * instability, defined by the equation: A + I = 1.
 *
 * <p><strong>Zone definitions:</strong>
 * <ul>
 *   <li><strong>ZONE_OF_PAIN:</strong> Concrete and stable (low A, low I).
 *       These packages are rigid and difficult to change because they contain
 *       concrete implementations that many other packages depend on.</li>
 *   <li><strong>ZONE_OF_USELESSNESS:</strong> Abstract and unstable (high A, high I).
 *       These packages contain abstractions that nobody uses. They add complexity
 *       without providing value.</li>
 *   <li><strong>MAIN_SEQUENCE:</strong> Balanced packages that lie close to the
 *       ideal line (A + I ≈ 1). These packages have an appropriate balance
 *       between abstractness and instability.</li>
 *   <li><strong>IDEAL:</strong> Perfect packages exactly on the main sequence
 *       (|A + I - 1| = 0). These packages exhibit optimal design characteristics.</li>
 * </ul>
 *
 * <p><strong>Thresholds for categorization:</strong>
 * <ul>
 *   <li>IDEAL: Distance = 0.0</li>
 *   <li>MAIN_SEQUENCE: 0.0 &lt; Distance ≤ 0.3</li>
 *   <li>ZONE_OF_PAIN: Distance &gt; 0.3 AND I &lt; 0.5</li>
 *   <li>ZONE_OF_USELESSNESS: Distance &gt; 0.3 AND I ≥ 0.5</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum ZoneCategory {

    /**
     * Zone of Pain: Concrete and stable packages.
     *
     * <p>These packages are characterized by:
     * <ul>
     *   <li>Low abstractness (mostly concrete implementations)</li>
     *   <li>Low instability (many packages depend on them)</li>
     *   <li>High rigidity (changes ripple through many dependents)</li>
     * </ul>
     *
     * <p><strong>Indicators:</strong> Distance &gt; 0.3 AND Instability &lt; 0.5
     *
     * <p><strong>Remediation:</strong>
     * <ul>
     *   <li>Extract interfaces from concrete classes</li>
     *   <li>Apply Dependency Inversion Principle</li>
     *   <li>Reduce incoming dependencies through abstractions</li>
     * </ul>
     */
    ZONE_OF_PAIN,

    /**
     * Zone of Uselessness: Abstract and unstable packages.
     *
     * <p>These packages are characterized by:
     * <ul>
     *   <li>High abstractness (mostly interfaces and abstract classes)</li>
     *   <li>High instability (few packages depend on them)</li>
     *   <li>Low utility (abstractions without significant usage)</li>
     * </ul>
     *
     * <p><strong>Indicators:</strong> Distance &gt; 0.3 AND Instability ≥ 0.5
     *
     * <p><strong>Remediation:</strong>
     * <ul>
     *   <li>Remove unused abstractions</li>
     *   <li>Merge abstractions with their implementations</li>
     *   <li>Increase concrete usage or remove the package</li>
     * </ul>
     */
    ZONE_OF_USELESSNESS,

    /**
     * Main Sequence: Well-balanced packages.
     *
     * <p>These packages exhibit a good balance between abstractness and
     * instability. They are neither too rigid nor too abstract, making them
     * maintainable and evolvable.
     *
     * <p><strong>Indicators:</strong> 0.0 &lt; Distance ≤ 0.3
     *
     * <p>Packages in this zone generally don't require immediate refactoring,
     * though monitoring is recommended to prevent drift into problematic zones.
     */
    MAIN_SEQUENCE,

    /**
     * Ideal: Perfect balance (exactly on the main sequence).
     *
     * <p>These packages have achieved the theoretical ideal where A + I = 1.
     * They demonstrate optimal design characteristics with exactly the right
     * balance of abstraction for their dependency profile.
     *
     * <p><strong>Indicators:</strong> Distance = 0.0
     *
     * <p>While ideal packages are excellent, perfection isn't always necessary.
     * Packages on the MAIN_SEQUENCE are also considered healthy.
     */
    IDEAL;

    /**
     * Determines the zone category based on metrics.
     *
     * <p>This method applies Robert C. Martin's classification rules to
     * categorize a package based on its distance from the main sequence
     * and its instability metric.
     *
     * @param distance    the normalized distance from the main sequence [0.0, 1.0]
     * @param instability the instability metric [0.0, 1.0]
     * @return the appropriate zone category
     */
    public static ZoneCategory categorize(double distance, double instability) {
        if (distance == 0.0) {
            return IDEAL;
        }

        if (distance <= 0.3) {
            return MAIN_SEQUENCE;
        }

        // Distance > 0.3: problematic zone
        if (instability < 0.5) {
            return ZONE_OF_PAIN;
        } else {
            return ZONE_OF_USELESSNESS;
        }
    }

    /**
     * Checks if this zone represents a healthy package.
     *
     * <p>A package is considered healthy if it's either IDEAL or on the MAIN_SEQUENCE.
     *
     * @return true if this is a healthy zone
     */
    public boolean isHealthy() {
        return this == IDEAL || this == MAIN_SEQUENCE;
    }

    /**
     * Checks if this zone represents a problematic package.
     *
     * <p>A package is problematic if it's in the ZONE_OF_PAIN or ZONE_OF_USELESSNESS.
     *
     * @return true if this is a problematic zone
     */
    public boolean isProblematic() {
        return this == ZONE_OF_PAIN || this == ZONE_OF_USELESSNESS;
    }
}
