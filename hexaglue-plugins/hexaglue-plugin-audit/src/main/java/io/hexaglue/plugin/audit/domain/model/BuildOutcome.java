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
 * Overall outcome of the audit execution.
 *
 * @since 1.0.0
 */
public enum BuildOutcome {
    /**
     * Audit passed - no blocking violations found.
     */
    SUCCESS,

    /**
     * Audit failed - blocking or critical violations found.
     */
    FAIL
}
