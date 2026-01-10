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

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;

/**
 * Calculates port coverage metrics for aggregates.
 *
 * <p>This calculator measures what percentage of aggregate roots have associated
 * repository ports. In DDD, aggregates are the unit of retrieval and should have
 * dedicated repository interfaces for persistence operations.
 *
 * <p><strong>Metric:</strong> port.coverage<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if < 100%<br>
 * <strong>Interpretation:</strong> Should be 100%. Every aggregate root should have
 * a corresponding repository port for persistence operations. Missing repositories
 * may indicate incomplete domain modeling or infrastructure concerns leaking into
 * the domain.
 *
 * @since 1.0.0
 */
public class PortCoverageMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "port.coverage";
    private static final double EXPECTED_COVERAGE = 100.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    @Override
    public Metric calculate(Codebase codebase) {
        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        if (aggregates.isEmpty()) {
            return Metric.of(
                    METRIC_NAME, 100.0, "%", "Percentage of aggregates with repository ports (no aggregates found)");
        }

        List<CodeUnit> repositories = codebase.unitsWithRole(RoleClassification.REPOSITORY);

        // Calculate how many aggregates have repositories
        long aggregatesWithRepo = aggregates.stream()
                .filter(agg -> hasRepository(agg, repositories))
                .count();

        double coverage = (double) aggregatesWithRepo / aggregates.size() * 100.0;

        return Metric.of(
                METRIC_NAME,
                coverage,
                "%",
                "Percentage of aggregates with repository ports",
                MetricThreshold.lessThan(EXPECTED_COVERAGE));
    }

    /**
     * Checks if an aggregate has an associated repository.
     *
     * <p>A repository is considered associated if its name contains the aggregate's
     * simple name followed by "Repository" (e.g., OrderRepository for Order aggregate).
     *
     * @param aggregate the aggregate to check
     * @param repositories list of all repositories
     * @return true if the aggregate has a repository
     */
    private boolean hasRepository(CodeUnit aggregate, List<CodeUnit> repositories) {
        String expectedRepoName = aggregate.simpleName() + "Repository";

        return repositories.stream().anyMatch(repo -> repo.simpleName().equals(expectedRepoName));
    }
}
