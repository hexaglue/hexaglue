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

package io.hexaglue.arch.model.audit;

import java.time.Duration;
import java.time.Instant;

/**
 * Metadata about an audit execution.
 *
 * @param timestamp         when the audit was performed
 * @param hexaglueVersion   the HexaGlue version used
 * @param analysisDuration  how long the analysis took
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record AuditMetadata(Instant timestamp, String hexaglueVersion, Duration analysisDuration) {

    /**
     * Returns a human-readable duration string.
     *
     * @return formatted duration
     */
    public String formattedDuration() {
        long seconds = analysisDuration.getSeconds();
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes " + (seconds % 60) + " seconds";
        } else {
            return (seconds / 3600) + " hours " + ((seconds % 3600) / 60) + " minutes";
        }
    }
}
