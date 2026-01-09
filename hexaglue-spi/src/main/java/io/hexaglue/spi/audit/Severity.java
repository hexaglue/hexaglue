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
 * <p>Severity levels help prioritize issues and determine whether the audit passes or fails:
 * <ul>
 *   <li><b>INFO</b>: Informational message, does not fail the audit</li>
 *   <li><b>WARNING</b>: Issue that should be addressed, does not fail the audit</li>
 *   <li><b>ERROR</b>: Critical issue that fails the audit</li>
 * </ul>
 *
 * @since 3.0.0
 */
public enum Severity {

    /**
     * Informational message.
     * Does not indicate a problem, just provides useful information.
     */
    INFO,

    /**
     * Warning level issue.
     * Indicates a potential problem that should be reviewed but doesn't fail the audit.
     */
    WARNING,

    /**
     * Error level issue.
     * Indicates a serious problem that causes the audit to fail.
     */
    ERROR
}
