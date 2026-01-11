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
 * Classification of a package in the Abstractness-Instability plane.
 *
 * <p>Based on Robert C. Martin's package metrics from "Agile Software Development:
 * Principles, Patterns, and Practices".
 *
 * <p>The Main Sequence is the ideal line where A + I = 1. Packages on or near
 * this line have a good balance between abstractness and stability.
 *
 * <p>Zone interpretation:
 * <ul>
 *   <li><b>IDEAL</b>: Perfect balance (D = 0.0)</li>
 *   <li><b>MAIN_SEQUENCE</b>: Close to ideal (D &le; 0.3)</li>
 *   <li><b>ZONE_OF_PAIN</b>: Stable and concrete - hard to change</li>
 *   <li><b>ZONE_OF_USELESSNESS</b>: Unstable and abstract - unused abstractions</li>
 * </ul>
 *
 * @since 3.0.0
 */
public enum ZoneClassification {

    /**
     * Perfect balance on the Main Sequence (D = 0.0).
     *
     * <p>The package has an ideal ratio of abstractness to stability.
     */
    IDEAL,

    /**
     * Close to the Main Sequence (D &le; 0.3).
     *
     * <p>Healthy packages with good balance.
     */
    MAIN_SEQUENCE,

    /**
     * Zone of Pain: Stable and Concrete (D &gt; 0.3 and I &lt; 0.5).
     *
     * <p>These packages are hard to change because:
     * <ul>
     *   <li>Many other packages depend on them (stable)</li>
     *   <li>They are concrete, requiring changes when dependencies change</li>
     * </ul>
     *
     * <p>Examples: Utility classes, data access objects with hardcoded logic.
     */
    ZONE_OF_PAIN,

    /**
     * Zone of Uselessness: Unstable and Abstract (D &gt; 0.3 and I &ge; 0.5).
     *
     * <p>These packages are problematic because:
     * <ul>
     *   <li>They are highly abstract (interfaces, abstract classes)</li>
     *   <li>Few or no other packages depend on them</li>
     * </ul>
     *
     * <p>Examples: Unused interfaces, abandoned abstractions.
     */
    ZONE_OF_USELESSNESS;

    /**
     * Classifies a package based on its metrics.
     *
     * <p>Algorithm:
     * <pre>
     * D = |A + I - 1|  (distance from Main Sequence)
     *
     * if D == 0.0           -&gt; IDEAL
     * else if D &le; 0.3    -&gt; MAIN_SEQUENCE
     * else if I &lt; 0.5     -&gt; ZONE_OF_PAIN (stable + concrete)
     * else                  -&gt; ZONE_OF_USELESSNESS (unstable + abstract)
     * </pre>
     *
     * @param distance    distance from main sequence |A + I - 1|
     * @param instability instability metric Ce / (Ca + Ce)
     * @return the zone classification
     */
    public static ZoneClassification classify(double distance, double instability) {
        if (distance == 0.0) {
            return IDEAL;
        }
        if (distance <= 0.3) {
            return MAIN_SEQUENCE;
        }
        if (instability < 0.5) {
            return ZONE_OF_PAIN;
        }
        return ZONE_OF_USELESSNESS;
    }

    /**
     * Returns true if this zone indicates a problematic package.
     *
     * @return true for ZONE_OF_PAIN or ZONE_OF_USELESSNESS
     */
    public boolean isProblematic() {
        return this == ZONE_OF_PAIN || this == ZONE_OF_USELESSNESS;
    }
}
