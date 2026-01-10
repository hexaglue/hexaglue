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
 * Severity level for constraint violations.
 *
 * <p>The severity levels are ordered from most to least severe:
 * <ol>
 *   <li>{@link #BLOCKER} - Build fails immediately, non-overridable</li>
 *   <li>{@link #CRITICAL} - Build fails unless explicitly allowed in configuration</li>
 *   <li>{@link #MAJOR} - Warning, build continues, should be fixed</li>
 *   <li>{@link #MINOR} - Nice to fix, low priority</li>
 *   <li>{@link #INFO} - Informational only, no action required</li>
 * </ol>
 *
 * @since 1.0.0
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
