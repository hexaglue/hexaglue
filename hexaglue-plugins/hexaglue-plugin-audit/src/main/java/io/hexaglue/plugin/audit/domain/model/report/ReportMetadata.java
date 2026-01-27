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

import java.time.Instant;
import java.util.Objects;

/**
 * Metadata about the audit report generation.
 *
 * @param projectName name of the project being audited
 * @param projectVersion version of the project
 * @param timestamp when the audit was performed
 * @param duration how long the audit took (e.g., "42ms")
 * @param hexaglueVersion version of HexaGlue used
 * @param pluginVersion version of the audit plugin
 * @since 5.0.0
 */
public record ReportMetadata(
        String projectName,
        String projectVersion,
        Instant timestamp,
        String duration,
        String hexaglueVersion,
        String pluginVersion) {

    /**
     * Creates report metadata with validation.
     */
    public ReportMetadata {
        Objects.requireNonNull(projectName, "projectName is required");
        Objects.requireNonNull(timestamp, "timestamp is required");
        Objects.requireNonNull(hexaglueVersion, "hexaglueVersion is required");
        Objects.requireNonNull(pluginVersion, "pluginVersion is required");
        if (projectVersion == null) {
            projectVersion = "unknown";
        }
        if (duration == null) {
            duration = "N/A";
        }
    }
}
