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
 * Severity level for audit rule violations.
 *
 * <p>Severity levels are ordered from most to least severe:
 * <ol>
 *   <li><b>BLOCKER</b>: Build fails immediately, cannot be overridden</li>
 *   <li><b>CRITICAL</b>: Build fails unless explicitly allowed in configuration</li>
 *   <li><b>MAJOR</b>: Warning, build continues, should be fixed</li>
 *   <li><b>MINOR</b>: Low priority issue, nice to fix</li>
 *   <li><b>INFO</b>: Informational only, no action required</li>
 * </ol>
 *
 * @since 3.0.0
 * @since 5.0.0 - Enriched with BLOCKER, CRITICAL, MAJOR, MINOR for actionable severity levels
 */
public enum Severity {

    /**
     * Build fails immediately. Cannot be overridden.
     * Used for critical architectural violations (e.g., circular aggregate dependencies).
     */
    BLOCKER,

    /**
     * Build fails unless explicitly allowed in configuration.
     * Used for serious DDD violations (e.g., mutable value objects, missing entity identity).
     */
    CRITICAL,

    /**
     * Warning level. Build continues but violation should be fixed.
     * Used for important architectural issues (e.g., aggregate without repository).
     */
    MAJOR,

    /**
     * Low priority issue. Nice to fix.
     * Used for best practice violations (e.g., large aggregates).
     */
    MINOR,

    /**
     * Informational only. No action required.
     * Used for documentation and metrics.
     */
    INFO
}
