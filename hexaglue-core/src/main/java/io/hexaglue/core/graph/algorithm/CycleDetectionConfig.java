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

package io.hexaglue.core.graph.algorithm;

/**
 * Configuration for cycle detection.
 *
 * <p>This configuration controls the behavior and performance of cycle detection:
 * <ul>
 *   <li>{@code maxCycles} - Maximum number of cycles to detect (prevents infinite loops)</li>
 *   <li>{@code maxDependenciesPerEdge} - Maximum edges to include per cycle (limits memory)</li>
 *   <li>{@code includeTransitive} - Whether to include transitive dependencies</li>
 * </ul>
 *
 * @param maxCycles               maximum number of cycles to detect
 * @param maxDependenciesPerEdge maximum edges to include per cycle
 * @param includeTransitive      whether to include transitive dependencies
 * @since 3.0.0
 */
public record CycleDetectionConfig(int maxCycles, int maxDependenciesPerEdge, boolean includeTransitive) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if maxCycles or maxDependenciesPerEdge is not positive
     */
    public CycleDetectionConfig {
        if (maxCycles <= 0) {
            throw new IllegalArgumentException("maxCycles must be positive");
        }
        if (maxDependenciesPerEdge <= 0) {
            throw new IllegalArgumentException("maxDependenciesPerEdge must be positive");
        }
    }

    /**
     * Returns the default configuration.
     *
     * <p>Defaults:
     * <ul>
     *   <li>maxCycles: 100</li>
     *   <li>maxDependenciesPerEdge: 10</li>
     *   <li>includeTransitive: false</li>
     * </ul>
     *
     * @return default configuration
     */
    public static CycleDetectionConfig defaults() {
        return new CycleDetectionConfig(100, 10, false);
    }

    /**
     * Creates a configuration for exhaustive cycle detection.
     *
     * <p>This configuration attempts to find all cycles with no limits,
     * which may be slow on large graphs.
     *
     * @return exhaustive configuration
     */
    public static CycleDetectionConfig exhaustive() {
        return new CycleDetectionConfig(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
    }

    /**
     * Creates a configuration for fast cycle detection.
     *
     * <p>This configuration finds only a few cycles quickly,
     * suitable for large graphs where detailed analysis is not needed.
     *
     * @return fast configuration
     */
    public static CycleDetectionConfig fast() {
        return new CycleDetectionConfig(10, 5, false);
    }
}
