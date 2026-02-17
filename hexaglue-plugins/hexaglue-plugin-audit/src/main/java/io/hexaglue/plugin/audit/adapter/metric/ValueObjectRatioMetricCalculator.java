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

package io.hexaglue.plugin.audit.adapter.metric;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;

/**
 * Calculates the ratio of Value Objects to total domain model types (Entities + Value Objects).
 *
 * <p>A high Value Object ratio indicates mature DDD modeling with a preference for
 * immutable, identity-less types. The DDD community considers that well-modeled domains
 * typically have more Value Objects than Entities (Vaughn Vernon, "Implementing DDD").
 *
 * <p><strong>Metric:</strong> ddd.value.object.ratio<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if &lt; 40%<br>
 * <strong>Interpretation:</strong> Higher is generally better. A ratio below 20% suggests
 * an entity-centric or anemic domain model where primitive fields could be refactored
 * into Value Objects.
 *
 * @since 5.1.0
 */
public class ValueObjectRatioMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "ddd.value.object.ratio";
    private static final double WARNING_THRESHOLD = 40.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the Value Object ratio using the v5 ArchType API.
     *
     * <p>Formula: {@code VO / (Entities + VO) * 100}
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.1.0
     */
    @Override
    public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery) {
        return model.domainIndex()
                .map(domain -> {
                    long entityCount = domain.entities().count();
                    long voCount = domain.valueObjects().count();
                    long total = entityCount + voCount;

                    if (total == 0) {
                        return Metric.of(
                                METRIC_NAME, 0.0, "%", "Value Object ratio (no entities or value objects found)");
                    }

                    double ratio = (double) voCount / total * 100.0;

                    return Metric.of(
                            METRIC_NAME,
                            ratio,
                            "%",
                            String.format(
                                    "Value Object ratio: %d VO out of %d domain model types (entities + VO)",
                                    voCount, total),
                            MetricThreshold.lessThan(WARNING_THRESHOLD));
                })
                .orElse(Metric.of(METRIC_NAME, 0.0, "%", "Value Object ratio (domain index not available)"));
    }
}
