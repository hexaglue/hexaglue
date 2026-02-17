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
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the average number of cross-aggregate dependencies per aggregate.
 *
 * <p>Each direct dependency between types belonging to different aggregates
 * represents a risk of transactional coupling. This metric counts such
 * cross-aggregate dependencies and divides by the number of aggregates.
 *
 * <p><strong>Metric:</strong> ddd.aggregate.coupling<br>
 * <strong>Unit:</strong> deps/aggregate<br>
 * <strong>Threshold:</strong> Warning if &gt; 3.0 average cross-aggregate deps<br>
 * <strong>Interpretation:</strong> Lower is better. High values indicate that
 * aggregates are not properly isolated and changes to one aggregate risk
 * cascading to others.
 *
 * @since 5.1.0
 */
public class InterAggregateCouplingMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "ddd.aggregate.coupling";
    private static final double WARNING_THRESHOLD = 3.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the average cross-aggregate coupling.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>List all aggregate roots from the DomainIndex</li>
     *   <li>For each aggregate, collect its member types (root + entities + value objects)</li>
     *   <li>Build a memberToAggregate map</li>
     *   <li>Count dependencies where source and target belong to different aggregates</li>
     *   <li>Return total_cross_deps / number_of_aggregates</li>
     * </ol>
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.1.0
     */
    @Override
    public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery) {
        if (model.domainIndex().isEmpty()) {
            return Metric.of(METRIC_NAME, 0.0, "deps/aggregate", "Inter-aggregate coupling (no domain index)");
        }

        DomainIndex domain = model.domainIndex().get();
        List<AggregateRoot> aggregates = domain.aggregateRoots().toList();

        if (aggregates.size() < 2) {
            return Metric.of(
                    METRIC_NAME,
                    0.0,
                    "deps/aggregate",
                    "Inter-aggregate coupling (fewer than 2 aggregates)",
                    MetricThreshold.greaterThan(WARNING_THRESHOLD));
        }

        // Build member-to-aggregate mapping
        Map<String, String> memberToAggregate = new HashMap<>();
        for (AggregateRoot aggregate : aggregates) {
            String rootQn = aggregate.id().qualifiedName();
            memberToAggregate.put(rootQn, rootQn);

            domain.entitiesOf(aggregate)
                    .forEach(entity -> memberToAggregate.put(entity.id().qualifiedName(), rootQn));

            domain.valueObjectsOf(aggregate)
                    .forEach(vo -> memberToAggregate.put(vo.id().qualifiedName(), rootQn));
        }

        // Count cross-aggregate dependencies
        if (architectureQuery == null) {
            return Metric.of(
                    METRIC_NAME, 0.0, "deps/aggregate", "Inter-aggregate coupling (architecture query not available)");
        }

        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();
        Set<String> aggregateMembers = memberToAggregate.keySet();
        int crossDeps = 0;

        for (String source : aggregateMembers) {
            String sourceAggregate = memberToAggregate.get(source);
            Set<String> targets = dependencies.getOrDefault(source, Set.of());
            for (String target : targets) {
                String targetAggregate = memberToAggregate.get(target);
                if (targetAggregate != null && !sourceAggregate.equals(targetAggregate)) {
                    crossDeps++;
                }
            }
        }

        double coupling = (double) crossDeps / aggregates.size();

        return Metric.of(
                METRIC_NAME,
                coupling,
                "deps/aggregate",
                String.format(
                        "Inter-aggregate coupling: %.1f avg (%d cross-deps across %d aggregates)",
                        coupling, crossDeps, aggregates.size()),
                MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }
}
