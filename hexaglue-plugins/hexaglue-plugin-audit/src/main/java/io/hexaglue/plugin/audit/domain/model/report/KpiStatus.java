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
 * Status of a KPI indicator.
 *
 * @since 5.0.0
 */
public enum KpiStatus {
    /**
     * KPI value meets or exceeds threshold.
     */
    OK("On Target"),

    /**
     * KPI value is close to threshold but still acceptable.
     */
    WARNING("At Risk"),

    /**
     * KPI value is below acceptable threshold.
     */
    CRITICAL("Below Target");

    private final String label;

    KpiStatus(String label) {
        this.label = label;
    }

    /**
     * Returns a human-readable label for this status.
     *
     * @return the display label
     */
    public String label() {
        return label;
    }
}
