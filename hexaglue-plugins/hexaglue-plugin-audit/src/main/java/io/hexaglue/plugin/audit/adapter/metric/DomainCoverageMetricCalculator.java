/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.metric;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.LayerClassification;

/**
 * Calculates domain layer coverage metrics.
 *
 * <p>This calculator measures what percentage of types in the codebase are
 * classified as domain layer types. A healthy hexagonal architecture should
 * have a substantial proportion of types in the domain layer, as this is where
 * the core business logic resides.
 *
 * <p><strong>Metric:</strong> domain.coverage<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if < 30%<br>
 * <strong>Interpretation:</strong> Higher is generally better. Low domain coverage
 * may indicate that business logic is leaking into infrastructure or that the
 * domain model is anemic.
 *
 * @since 1.0.0
 */
public class DomainCoverageMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "domain.coverage";
    private static final double WARNING_THRESHOLD = 30.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    @Override
    public Metric calculate(Codebase codebase) {
        long totalTypes = codebase.units().size();

        if (totalTypes == 0) {
            return Metric.of(
                    METRIC_NAME, 0.0, "%", "Percentage of types in domain layer (no types found)");
        }

        long domainTypes = codebase.unitsInLayer(LayerClassification.DOMAIN).size();

        double coverage = (double) domainTypes / totalTypes * 100.0;

        return Metric.of(
                METRIC_NAME,
                coverage,
                "%",
                "Percentage of types in domain layer",
                MetricThreshold.lessThan(WARNING_THRESHOLD));
    }
}
