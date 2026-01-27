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

/**
 * Status of the audit report indicating whether the build passes or fails.
 *
 * @since 5.0.0
 */
public enum ReportStatus {
    /**
     * Audit passed - no blockers or critical issues found.
     */
    PASSED,

    /**
     * Audit failed - at least one blocker or critical issue found.
     */
    FAILED,

    /**
     * Audit passed with warnings - no blockers but some issues need attention.
     */
    PASSED_WITH_WARNINGS
}
