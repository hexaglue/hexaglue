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

import java.util.Objects;

/**
 * Effort estimate for a remediation action.
 *
 * @param days estimated effort in days
 * @param description description of what the effort includes
 * @since 5.0.0
 */
public record EffortEstimate(
        double days,
        String description) {

    /**
     * Creates an effort estimate with validation.
     */
    public EffortEstimate {
        if (days < 0) {
            throw new IllegalArgumentException("days cannot be negative");
        }
        Objects.requireNonNull(description, "description is required");
    }

    /**
     * Creates an effort estimate with just the days.
     *
     * @param days number of days
     * @return the estimate
     */
    public static EffortEstimate days(double days) {
        return new EffortEstimate(days, formatDescription(days));
    }

    /**
     * Creates an effort estimate with days and description.
     *
     * @param days number of days
     * @param description what the effort includes
     * @return the estimate
     */
    public static EffortEstimate of(double days, String description) {
        return new EffortEstimate(days, description);
    }

    private static String formatDescription(double days) {
        if (days < 1) {
            return String.format("%.1f day effort", days);
        }
        return String.format("%.1f days effort", days);
    }

    /**
     * Returns formatted effort string.
     *
     * @return formatted string (e.g., "2 days", "0.5 days")
     */
    public String formatted() {
        if (days == 1.0) {
            return "1 day";
        }
        if (days == (int) days) {
            return (int) days + " days";
        }
        return days + " days";
    }
}
