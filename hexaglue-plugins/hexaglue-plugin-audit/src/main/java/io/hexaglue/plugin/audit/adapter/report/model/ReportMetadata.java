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

package io.hexaglue.plugin.audit.adapter.report.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Metadata about the audit report generation.
 *
 * @param projectName     the name of the project audited
 * @param timestamp       when the audit was performed
 * @param duration        how long the audit took (formatted string like "1.23s")
 * @param hexaglueVersion the HexaGlue version used
 * @since 1.0.0
 */
public record ReportMetadata(String projectName, Instant timestamp, String duration, String hexaglueVersion) {

    public ReportMetadata {
        Objects.requireNonNull(projectName, "projectName required");
        Objects.requireNonNull(timestamp, "timestamp required");
        Objects.requireNonNull(duration, "duration required");
        Objects.requireNonNull(hexaglueVersion, "hexaglueVersion required");
    }
}
