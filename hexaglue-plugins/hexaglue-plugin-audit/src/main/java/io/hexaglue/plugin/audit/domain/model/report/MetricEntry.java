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
import java.util.Optional;

/**
 * A metric entry in the appendix.
 *
 * @param id metric identifier
 * @param name display name
 * @param value metric value
 * @param unit unit of measurement
 * @param threshold threshold configuration (may be null)
 * @param status status based on threshold comparison
 * @since 5.0.0
 */
public record MetricEntry(
        String id,
        String name,
        double value,
        String unit,
        MetricThreshold threshold,
        KpiStatus status) {

    /**
     * Creates a metric entry with validation.
     */
    public MetricEntry {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(status, "status is required");
        if (unit == null) {
            unit = "";
        }
    }

    /**
     * Threshold configuration for a metric.
     *
     * @param min minimum acceptable value (null if no minimum)
     * @param max maximum acceptable value (null if no maximum)
     */
    public record MetricThreshold(Double min, Double max) {

        /**
         * Creates a minimum-only threshold.
         *
         * @param min minimum value
         * @return threshold
         */
        public static MetricThreshold min(double min) {
            return new MetricThreshold(min, null);
        }

        /**
         * Creates a maximum-only threshold.
         *
         * @param max maximum value
         * @return threshold
         */
        public static MetricThreshold max(double max) {
            return new MetricThreshold(null, max);
        }

        /**
         * Creates a range threshold.
         *
         * @param min minimum value
         * @param max maximum value
         * @return threshold
         */
        public static MetricThreshold range(double min, double max) {
            return new MetricThreshold(min, max);
        }

        /**
         * Returns formatted threshold string.
         *
         * @return formatted string (e.g., "min 100%", "max 10")
         */
        public String formatted() {
            if (min != null && max != null) {
                return "min " + formatValue(min) + ", max " + formatValue(max);
            } else if (min != null) {
                return "min " + formatValue(min);
            } else if (max != null) {
                return "max " + formatValue(max);
            }
            return "";
        }

        private String formatValue(double value) {
            if (value == (int) value) {
                return String.valueOf((int) value);
            }
            return String.valueOf(value);
        }
    }

    /**
     * Returns threshold as optional.
     *
     * @return optional threshold
     */
    public Optional<MetricThreshold> thresholdOpt() {
        return Optional.ofNullable(threshold);
    }

    /**
     * Returns formatted value with unit.
     *
     * @return formatted string (e.g., "92.86%", "1.29")
     */
    public String formattedValue() {
        String valueStr;
        if (value == (int) value) {
            valueStr = String.valueOf((int) value);
        } else {
            valueStr = String.format("%.2f", value);
        }
        return valueStr + unit;
    }
}
