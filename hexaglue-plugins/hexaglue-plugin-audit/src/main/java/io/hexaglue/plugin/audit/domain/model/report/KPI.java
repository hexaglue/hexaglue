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
 * Key Performance Indicator for the audit verdict.
 *
 * @param id unique identifier (e.g., "ddd-compliance")
 * @param name display name (e.g., "DDD Compliance")
 * @param value current value
 * @param unit unit of measurement (e.g., "%", "ratio")
 * @param weight weight in the overall score (0-100)
 * @param threshold threshold value to compare against
 * @param status status based on comparison with threshold
 * @since 5.0.0
 */
public record KPI(String id, String name, double value, String unit, int weight, double threshold, KpiStatus status) {

    /**
     * Creates a KPI with validation.
     */
    public KPI {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(unit, "unit is required");
        Objects.requireNonNull(status, "status is required");
    }

    /**
     * Calculates the contribution to the overall score.
     *
     * @return contribution value (value * weight / 100)
     */
    public double contribution() {
        return value * weight / 100.0;
    }

    /**
     * Creates a percentage KPI.
     *
     * @param id identifier
     * @param name display name
     * @param value percentage value (0-100)
     * @param weight weight in the overall score (0-100)
     * @param threshold minimum acceptable percentage
     * @return the KPI
     */
    public static KPI percentage(String id, String name, double value, int weight, double threshold) {
        KpiStatus status = determineStatus(value, threshold);
        return new KPI(id, name, value, "%", weight, threshold, status);
    }

    private static KpiStatus determineStatus(double value, double threshold) {
        if (value >= threshold) {
            return KpiStatus.OK;
        } else if (value >= threshold * 0.8) {
            return KpiStatus.WARNING;
        } else {
            return KpiStatus.CRITICAL;
        }
    }
}
