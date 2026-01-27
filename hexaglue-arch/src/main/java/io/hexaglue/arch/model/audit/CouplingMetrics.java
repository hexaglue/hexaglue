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
 * Coupling metrics for a package or type.
 *
 * <p>These metrics are based on Robert C. Martin's package principles:
 * <ul>
 *   <li><b>Ca (Afferent Coupling)</b>: Number of classes outside the package
 *       that depend on classes inside the package</li>
 *   <li><b>Ce (Efferent Coupling)</b>: Number of classes inside the package
 *       that depend on classes outside the package</li>
 *   <li><b>I (Instability)</b>: Ce / (Ca + Ce). Range: 0 (stable) to 1 (unstable)</li>
 *   <li><b>A (Abstractness)</b>: Number of abstract types / Total types.
 *       Range: 0 (concrete) to 1 (abstract)</li>
 *   <li><b>D (Distance from Main Sequence)</b>: |A + I - 1|. Ideally close to 0</li>
 * </ul>
 *
 * @param packageName the package name
 * @param afferentCoupling incoming dependencies (Ca)
 * @param efferentCoupling outgoing dependencies (Ce)
 * @param abstractness ratio of abstract types (A)
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record CouplingMetrics(String packageName, int afferentCoupling, int efferentCoupling, double abstractness) {

    /**
     * Calculates instability (I = Ce / (Ca + Ce)).
     *
     * <p>Returns 0.0 if the package has no couplings.
     *
     * @return instability score (0.0 = stable, 1.0 = unstable)
     */
    public double instability() {
        int total = afferentCoupling + efferentCoupling;
        return total == 0 ? 0.0 : (double) efferentCoupling / total;
    }

    /**
     * Calculates distance from the main sequence (D = |A + I - 1|).
     *
     * <p>The main sequence represents the ideal balance between abstractness
     * and stability. Values close to 0 are ideal.
     *
     * @return distance from main sequence (0.0 = ideal)
     */
    public double distanceFromMainSequence() {
        return Math.abs(abstractness + instability() - 1.0);
    }

    /**
     * Returns true if this package is in the "Zone of Pain" (stable + concrete).
     *
     * <p>Zone of Pain: I < 0.3 and A < 0.3 (rigid, hard to change)
     */
    public boolean isInZoneOfPain() {
        return instability() < 0.3 && abstractness < 0.3;
    }

    /**
     * Returns true if this package is in the "Zone of Uselessness" (unstable + abstract).
     *
     * <p>Zone of Uselessness: I > 0.7 and A > 0.7 (abstract with no users)
     */
    public boolean isInZoneOfUselessness() {
        return instability() > 0.7 && abstractness > 0.7;
    }

    /**
     * Classifies this package into a zone based on Martin's metrics.
     *
     * <p>Algorithm:
     * <pre>
     * D = distanceFromMainSequence()
     * I = instability()
     *
     * if D == 0.0           -&gt; IDEAL
     * else if D &le; 0.3    -&gt; MAIN_SEQUENCE
     * else if I &lt; 0.5     -&gt; ZONE_OF_PAIN
     * else                  -&gt; ZONE_OF_USELESSNESS
     * </pre>
     *
     * @return the zone classification
     */
    public ZoneClassification zone() {
        return ZoneClassification.classify(distanceFromMainSequence(), instability());
    }

    /**
     * Returns true if this package is in a problematic zone.
     *
     * <p>A package is problematic if it's in the Zone of Pain or Zone of Uselessness.
     *
     * @return true for ZONE_OF_PAIN or ZONE_OF_USELESSNESS
     */
    public boolean isProblematic() {
        return zone().isProblematic();
    }
}
