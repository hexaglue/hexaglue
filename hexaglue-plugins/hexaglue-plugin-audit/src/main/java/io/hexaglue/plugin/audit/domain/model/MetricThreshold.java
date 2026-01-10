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

package io.hexaglue.plugin.audit.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Threshold for metric warnings.
 *
 * <p>Thresholds define acceptable ranges for metrics. A metric exceeds its threshold
 * when its value falls outside the acceptable range.
 *
 * @param max      the maximum acceptable value (empty if no upper limit)
 * @param min      the minimum acceptable value (empty if no lower limit)
 * @param operator the comparison operator
 * @since 1.0.0
 */
public record MetricThreshold(Optional<Double> max, Optional<Double> min, ThresholdOperator operator) {

    /**
     * Compact constructor with validation.
     */
    public MetricThreshold {
        Objects.requireNonNull(max, "max required (use Optional.empty() if none)");
        Objects.requireNonNull(min, "min required (use Optional.empty() if none)");
        Objects.requireNonNull(operator, "operator required");
    }

    /**
     * Factory method for creating a "greater than" threshold.
     *
     * @param maxValue the maximum acceptable value
     * @return a new MetricThreshold
     */
    public static MetricThreshold greaterThan(double maxValue) {
        return new MetricThreshold(Optional.of(maxValue), Optional.empty(), ThresholdOperator.GREATER_THAN);
    }

    /**
     * Factory method for creating a "less than" threshold.
     *
     * @param minValue the minimum acceptable value
     * @return a new MetricThreshold
     */
    public static MetricThreshold lessThan(double minValue) {
        return new MetricThreshold(Optional.empty(), Optional.of(minValue), ThresholdOperator.LESS_THAN);
    }

    /**
     * Factory method for creating a "between" threshold.
     *
     * @param minValue the minimum acceptable value
     * @param maxValue the maximum acceptable value
     * @return a new MetricThreshold
     */
    public static MetricThreshold between(double minValue, double maxValue) {
        return new MetricThreshold(Optional.of(maxValue), Optional.of(minValue), ThresholdOperator.BETWEEN);
    }

    /**
     * Checks if the given value exceeds this threshold.
     *
     * @param value the value to check
     * @return true if the value exceeds the threshold
     */
    public boolean isExceeded(double value) {
        return switch (operator) {
            case GREATER_THAN -> max.isPresent() && value > max.get();
            case LESS_THAN -> min.isPresent() && value < min.get();
            case BETWEEN -> (min.isPresent() && value < min.get()) || (max.isPresent() && value > max.get());
        };
    }
}
