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
 * A quality or architecture metric.
 *
 * <p>Metrics provide quantitative measurements of code quality and architectural
 * properties. Examples include:
 * <ul>
 *   <li>Average aggregate size (methods per aggregate)</li>
 *   <li>Coupling metrics (NCCD, afferent/efferent coupling)</li>
 *   <li>Cohesion metrics (LCOM)</li>
 *   <li>Complexity metrics (cyclomatic complexity)</li>
 * </ul>
 *
 * @param name        the metric name (e.g., "aggregate.avgSize")
 * @param value       the numeric value
 * @param unit        the unit of measurement (e.g., "methods", "ratio", "complexity")
 * @param description the human-readable description
 * @param threshold   the optional threshold for warnings
 * @since 1.0.0
 */
public record Metric(String name, double value, String unit, String description, Optional<MetricThreshold> threshold) {

    /**
     * Compact constructor with validation.
     */
    public Metric {
        Objects.requireNonNull(name, "name required");
        Objects.requireNonNull(unit, "unit required");
        Objects.requireNonNull(description, "description required");
        Objects.requireNonNull(threshold, "threshold required (use Optional.empty() if none)");
    }

    /**
     * Factory method for creating a metric without a threshold.
     *
     * @param name        the metric name
     * @param value       the value
     * @param unit        the unit
     * @param description the description
     * @return a new Metric instance
     */
    public static Metric of(String name, double value, String unit, String description) {
        return new Metric(name, value, unit, description, Optional.empty());
    }

    /**
     * Factory method for creating a metric with a threshold.
     *
     * @param name        the metric name
     * @param value       the value
     * @param unit        the unit
     * @param description the description
     * @param threshold   the threshold
     * @return a new Metric instance
     */
    public static Metric of(String name, double value, String unit, String description, MetricThreshold threshold) {
        return new Metric(name, value, unit, description, Optional.of(threshold));
    }

    /**
     * Returns true if this metric exceeds its threshold (if present).
     *
     * @return true if threshold is present and exceeded
     */
    public boolean exceedsThreshold() {
        return threshold.isPresent() && threshold.get().isExceeded(value);
    }
}
