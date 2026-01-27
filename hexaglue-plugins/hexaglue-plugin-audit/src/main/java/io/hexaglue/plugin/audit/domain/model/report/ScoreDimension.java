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

import java.util.Map;
import java.util.Optional;

/**
 * A dimension of the overall score breakdown.
 *
 * @param weight weight of this dimension in the total score (percentage)
 * @param score score for this dimension (0-100)
 * @param contribution contribution to the total score (weight * score / 100)
 * @param details optional additional details
 * @since 5.0.0
 */
public record ScoreDimension(
        int weight,
        int score,
        double contribution,
        Map<String, String> details) {

    /**
     * Creates a score dimension with validation.
     */
    public ScoreDimension {
        if (weight < 0 || weight > 100) {
            throw new IllegalArgumentException("weight must be between 0 and 100");
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        details = details != null ? Map.copyOf(details) : Map.of();
    }

    /**
     * Creates a score dimension without details.
     *
     * @param weight weight percentage
     * @param score score value
     * @return the dimension
     */
    public static ScoreDimension of(int weight, int score) {
        double contribution = (weight * score) / 100.0;
        return new ScoreDimension(weight, score, contribution, Map.of());
    }

    /**
     * Creates a score dimension with details.
     *
     * @param weight weight percentage
     * @param score score value
     * @param details additional details
     * @return the dimension
     */
    public static ScoreDimension of(int weight, int score, Map<String, String> details) {
        double contribution = (weight * score) / 100.0;
        return new ScoreDimension(weight, score, contribution, details);
    }

    /**
     * Returns details as optional.
     *
     * @return optional details (empty if no details)
     */
    public Optional<Map<String, String>> detailsOpt() {
        return details.isEmpty() ? Optional.empty() : Optional.of(details);
    }
}
