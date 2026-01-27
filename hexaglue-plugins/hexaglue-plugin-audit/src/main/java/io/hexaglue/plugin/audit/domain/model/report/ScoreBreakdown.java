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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * Breakdown of the overall score into dimensions.
 *
 * @param dddCompliance DDD compliance dimension
 * @param hexagonalCompliance hexagonal architecture compliance dimension
 * @param dependencyQuality dependency quality dimension
 * @param couplingMetrics coupling metrics dimension
 * @param cohesionQuality cohesion quality dimension
 * @since 5.0.0
 */
public record ScoreBreakdown(
        ScoreDimension dddCompliance,
        ScoreDimension hexagonalCompliance,
        ScoreDimension dependencyQuality,
        ScoreDimension couplingMetrics,
        ScoreDimension cohesionQuality) {

    /**
     * Creates a score breakdown with validation.
     */
    public ScoreBreakdown {
        Objects.requireNonNull(dddCompliance, "dddCompliance is required");
        Objects.requireNonNull(hexagonalCompliance, "hexagonalCompliance is required");
        Objects.requireNonNull(dependencyQuality, "dependencyQuality is required");
        Objects.requireNonNull(couplingMetrics, "couplingMetrics is required");
        Objects.requireNonNull(cohesionQuality, "cohesionQuality is required");
    }

    /**
     * Calculates the total score from all dimensions.
     *
     * @return total score (0-100)
     */
    public double totalScore() {
        return dddCompliance.contribution()
                + hexagonalCompliance.contribution()
                + dependencyQuality.contribution()
                + couplingMetrics.contribution()
                + cohesionQuality.contribution();
    }

    /**
     * Returns the total score as an integer.
     *
     * @return rounded total score
     */
    public int totalScoreRounded() {
        return (int) Math.round(totalScore());
    }

    /**
     * Validates that weights sum to 100.
     *
     * @return true if weights are valid
     */
    public boolean hasValidWeights() {
        int totalWeight = dddCompliance.weight()
                + hexagonalCompliance.weight()
                + dependencyQuality.weight()
                + couplingMetrics.weight()
                + cohesionQuality.weight();
        return totalWeight == 100;
    }
}
