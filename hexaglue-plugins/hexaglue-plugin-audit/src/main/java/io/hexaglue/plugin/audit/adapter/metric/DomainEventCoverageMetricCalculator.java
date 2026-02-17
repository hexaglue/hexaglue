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
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.List;

/**
 * Calculates the percentage of aggregate roots that emit at least one domain event.
 *
 * <p>Domain events are the primary mechanism for inter-aggregate communication in DDD.
 * Low event coverage suggests aggregates may be communicating through direct coupling
 * rather than through events, which limits scalability and decoupling.
 *
 * <p><strong>Metric:</strong> ddd.event.coverage<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if &lt; 50%<br>
 * <strong>Interpretation:</strong> Higher is better. Above 80% indicates a mature
 * event-driven architecture. Below 20% suggests direct inter-aggregate coupling.
 *
 * @since 5.1.0
 */
public class DomainEventCoverageMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "ddd.event.coverage";
    private static final double WARNING_THRESHOLD = 50.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the domain event coverage using the v5 ArchType API.
     *
     * <p>An aggregate "emits" events if its {@link AggregateRoot#domainEvents()} list
     * is non-empty.
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
                    List<AggregateRoot> aggregates = domain.aggregateRoots().toList();

                    if (aggregates.isEmpty()) {
                        return Metric.of(METRIC_NAME, 0.0, "%", "Domain event coverage (no aggregates found)");
                    }

                    long aggregatesWithEvents = aggregates.stream()
                            .filter(agg -> !agg.domainEvents().isEmpty())
                            .count();

                    double coverage = (double) aggregatesWithEvents / aggregates.size() * 100.0;

                    return Metric.of(
                            METRIC_NAME,
                            coverage,
                            "%",
                            String.format(
                                    "Domain event coverage: %d of %d aggregates emit events",
                                    aggregatesWithEvents, aggregates.size()),
                            MetricThreshold.lessThan(WARNING_THRESHOLD));
                })
                .orElse(Metric.of(METRIC_NAME, 0.0, "%", "Domain event coverage (domain index not available)"));
    }
}
