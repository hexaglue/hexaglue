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
 * Architecture-specific metrics for the codebase.
 *
 * @param componentCount             number of architectural components
 * @param couplingBetweenComponents  average coupling between components (0-1)
 * @param cohesionWithinComponents   average cohesion within components (0-1)
 * @param circularDependencies       number of circular dependencies detected
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record ArchitectureMetrics(
        int componentCount,
        double couplingBetweenComponents,
        double cohesionWithinComponents,
        int circularDependencies) {

    /**
     * Returns true if coupling is acceptable (low).
     *
     * @return true if coupling < 0.5
     */
    public boolean hasLowCoupling() {
        return couplingBetweenComponents < 0.5;
    }

    /**
     * Returns true if cohesion is good (high).
     *
     * @return true if cohesion > 0.7
     */
    public boolean hasHighCohesion() {
        return cohesionWithinComponents > 0.7;
    }

    /**
     * Returns true if there are no circular dependencies.
     *
     * @return true if circularDependencies == 0
     */
    public boolean hasNoCircularDependencies() {
        return circularDependencies == 0;
    }
}
