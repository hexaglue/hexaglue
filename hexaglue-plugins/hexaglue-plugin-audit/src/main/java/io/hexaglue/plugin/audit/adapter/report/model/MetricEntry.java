/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.report.model;

import java.util.Objects;

/**
 * A metric entry in the audit report.
 *
 * @param name          the metric name (e.g., "aggregate.avgSize")
 * @param value         the measured value
 * @param unit          the unit of measurement (e.g., "methods", "ratio")
 * @param threshold     the threshold value (null if no threshold)
 * @param thresholdType the threshold type: "max" or "min" (null if no threshold)
 * @param status        the status: "OK", "WARNING", or "CRITICAL"
 * @since 1.0.0
 */
public record MetricEntry(
        String name, double value, String unit, Double threshold, String thresholdType, String status) {

    public MetricEntry {
        Objects.requireNonNull(name, "name required");
        Objects.requireNonNull(unit, "unit required");
        Objects.requireNonNull(status, "status required");

        if (threshold != null && thresholdType == null) {
            throw new IllegalArgumentException("thresholdType required when threshold is set");
        }
        if (thresholdType != null && !"max".equals(thresholdType) && !"min".equals(thresholdType)) {
            throw new IllegalArgumentException("thresholdType must be 'max' or 'min'");
        }
        if (!"OK".equals(status) && !"WARNING".equals(status) && !"CRITICAL".equals(status)) {
            throw new IllegalArgumentException("status must be 'OK', 'WARNING', or 'CRITICAL'");
        }
    }

    /**
     * Returns true if this metric exceeds its threshold.
     *
     * @return true if status is not OK
     */
    public boolean exceedsThreshold() {
        return !"OK".equals(status);
    }
}
