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

import java.util.Locale;

/**
 * Represents Robert C. Martin's package metrics and zone classification.
 *
 * <p>This record captures the key metrics used to assess package design quality
 * according to the principles outlined in "Agile Software Development: Principles,
 * Patterns, and Practices" by Robert C. Martin.
 *
 * <p><strong>Metrics definitions:</strong>
 * <ul>
 *   <li><strong>Abstractness (A):</strong> The ratio of abstract classes and interfaces
 *       to total classes. Range: [0.0, 1.0], where 0.0 = completely concrete, 1.0 = completely abstract.</li>
 *   <li><strong>Instability (I):</strong> The ratio of outgoing dependencies to total dependencies
 *       (incoming + outgoing). Range: [0.0, 1.0], where 0.0 = maximally stable, 1.0 = maximally unstable.</li>
 *   <li><strong>Distance (D):</strong> The normalized distance from the "Main Sequence" ideal.
 *       Calculated as |A + I - 1|. Range: [0.0, 1.0], where 0.0 = on the ideal line.</li>
 * </ul>
 *
 * <p><strong>Main Sequence:</strong> The theoretical ideal where A + I = 1. Packages on or
 * near this line have an optimal balance between abstractness and stability.
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * PackageZoneMetrics metrics = new PackageZoneMetrics(
 *     "com.example.domain",
 *     0.5,  // 50% abstract
 *     0.5,  // 50% unstable
 *     0.0,  // Perfect: |0.5 + 0.5 - 1| = 0
 *     ZoneCategory.IDEAL
 * );
 *
 * if (metrics.zone().isProblematic()) {
 *     System.out.println("Package needs refactoring!");
 * }
 * }</pre>
 *
 * @param packageName  the fully qualified package name
 * @param abstractness the abstractness metric (A), range [0.0, 1.0]
 * @param instability  the instability metric (I), range [0.0, 1.0]
 * @param distance     the distance from main sequence (D), range [0.0, 1.0]
 * @param zone         the categorized zone based on the metrics
 * @since 1.0.0
 */
public record PackageZoneMetrics(
        String packageName, double abstractness, double instability, double distance, ZoneCategory zone) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if any metric is out of valid range [0.0, 1.0]
     * @throws NullPointerException     if packageName or zone is null
     */
    public PackageZoneMetrics {
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName cannot be null or blank");
        }
        if (zone == null) {
            throw new NullPointerException("zone cannot be null");
        }
        if (abstractness < 0.0 || abstractness > 1.0) {
            throw new IllegalArgumentException("abstractness must be in range [0.0, 1.0]: " + abstractness);
        }
        if (instability < 0.0 || instability > 1.0) {
            throw new IllegalArgumentException("instability must be in range [0.0, 1.0]: " + instability);
        }
        if (distance < 0.0 || distance > 1.0) {
            throw new IllegalArgumentException("distance must be in range [0.0, 1.0]: " + distance);
        }
    }

    /**
     * Checks if this package is in a healthy zone.
     *
     * <p>A package is healthy if it's either IDEAL or on the MAIN_SEQUENCE.
     *
     * @return true if the package is healthy
     */
    public boolean isHealthy() {
        return zone.isHealthy();
    }

    /**
     * Checks if this package is in a problematic zone.
     *
     * <p>A package is problematic if it's in the ZONE_OF_PAIN or ZONE_OF_USELESSNESS.
     *
     * @return true if the package is problematic
     */
    public boolean isProblematic() {
        return zone.isProblematic();
    }

    /**
     * Returns a textual description of this package's health.
     *
     * @return a human-readable description
     */
    public String description() {
        return switch (zone) {
            case IDEAL ->
                String.format(
                        Locale.US,
                        "Package '%s' is ideal (A=%.2f, I=%.2f, D=%.2f). "
                                + "Perfect balance of abstraction and stability.",
                        packageName,
                        abstractness,
                        instability,
                        distance);
            case MAIN_SEQUENCE ->
                String.format(
                        Locale.US,
                        "Package '%s' is on the main sequence (A=%.2f, I=%.2f, D=%.2f). "
                                + "Good balance of abstraction and stability.",
                        packageName,
                        abstractness,
                        instability,
                        distance);
            case ZONE_OF_PAIN ->
                String.format(
                        Locale.US,
                        "Package '%s' is in the Zone of Pain (A=%.2f, I=%.2f, D=%.2f). "
                                + "Too concrete and stable - difficult to change.",
                        packageName,
                        abstractness,
                        instability,
                        distance);
            case ZONE_OF_USELESSNESS ->
                String.format(
                        Locale.US,
                        "Package '%s' is in the Zone of Uselessness (A=%.2f, I=%.2f, D=%.2f). "
                                + "Too abstract and unstable - unused abstractions.",
                        packageName,
                        abstractness,
                        instability,
                        distance);
        };
    }
}
