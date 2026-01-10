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
 * Priority level for architectural recommendations.
 *
 * <p>Priority levels are ordered from highest to lowest urgency:
 * <ol>
 *   <li>{@link #IMMEDIATE} - Critical issues requiring immediate action</li>
 *   <li>{@link #SHORT_TERM} - Important issues to address within weeks</li>
 *   <li>{@link #MEDIUM_TERM} - Improvements to plan for next iteration</li>
 *   <li>{@link #LOW} - Nice-to-have improvements with minimal impact</li>
 * </ol>
 *
 * <p>The priority is determined based on violation severity, architectural impact,
 * and the number of affected types.
 *
 * @since 1.0.0
 */
public enum RecommendationPriority {
    /**
     * Immediate action required.
     * Used for blocker/critical violations or architectural violations affecting system integrity.
     */
    IMMEDIATE,

    /**
     * Should be addressed in the short term (within weeks).
     * Used for major violations affecting multiple components.
     */
    SHORT_TERM,

    /**
     * Plan for next iteration (within months).
     * Used for major violations with isolated impact or accumulated minor issues.
     */
    MEDIUM_TERM,

    /**
     * Low priority enhancement.
     * Used for minor/info violations with minimal impact.
     */
    LOW
}
