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
 * Represents the trend direction of audit quality over time.
 *
 * <p>The trend is determined by comparing two consecutive audit results,
 * analyzing changes in health scores and violation counts.
 *
 * @since 1.0.0
 */
public enum AuditTrend {

    /**
     * Audit quality has improved.
     *
     * <p>This indicates that either the health score increased or the
     * number of violations decreased compared to the previous audit.
     */
    IMPROVED,

    /**
     * Audit quality has degraded.
     *
     * <p>This indicates that either the health score decreased or the
     * number of violations increased compared to the previous audit.
     */
    DEGRADED,

    /**
     * Audit quality remains stable.
     *
     * <p>This indicates no significant change in health score
     * (within ±1 point tolerance) and the same violation count.
     */
    STABLE
}
