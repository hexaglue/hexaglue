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
import io.hexaglue.arch.model.DomainType;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;

/**
 * Calculates average cyclomatic complexity for domain types.
 *
 * <p>Cyclomatic complexity measures the number of linearly independent paths through
 * a method's control flow. It counts decision points such as if, for, while, switch,
 * catch, and logical operators (&& and ||).
 *
 * <p><strong>Metric:</strong> code.complexity.average<br>
 * <strong>Unit:</strong> complexity<br>
 * <strong>Threshold:</strong> Warning if average > 10<br>
 * <strong>Interpretation:</strong> Lower is better. Higher complexity indicates methods
 * that may be difficult to understand, test, and maintain. Methods with complexity > 10
 * should be considered for refactoring.
 *
 * <h2>Complexity Calculation</h2>
 * <p>The cyclomatic complexity for each method is provided by the core analysis engine,
 * which analyzes method bodies to count:
 * <ul>
 *   <li>Conditional statements (if, else if, ternary ?:)</li>
 *   <li>Loop statements (for, while, do-while, foreach)</li>
 *   <li>Switch cases</li>
 *   <li>Catch blocks</li>
 *   <li>Logical operators (&& and ||)</li>
 * </ul>
 *
 * <h2>Scope</h2>
 * <p>This metric only analyzes domain layer types (aggregate roots, entities, value
 * objects, domain services) where complexity directly impacts business logic clarity.
 * Infrastructure and adapter code typically has different complexity patterns and is
 * excluded from this metric.
 *
 * <h2>Complexity Thresholds</h2>
 * <ul>
 *   <li>1-5: Simple, easy to understand</li>
 *   <li>6-10: Moderate complexity, acceptable</li>
 *   <li>11-20: High complexity, consider refactoring</li>
 *   <li>21+: Very high complexity, should be refactored</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CodeComplexityMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "code.complexity.average";
    private static final double WARNING_THRESHOLD = 10.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the average cyclomatic complexity using the v5 ArchType API.
     *
     * <p>This implementation calculates the average cyclomatic complexity across all
     * methods in domain types (aggregate roots, entities, value objects, domain services).
     * Methods without a calculated complexity value default to 1 (simple method).</p>
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.0.0
     */
    @Override
    public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery) {
        return model.domainIndex()
                .map(domain -> {
                    int totalMethods = 0;
                    int totalComplexity = 0;

                    // Process all domain types
                    for (DomainType domainType :
                            domain.aggregateRoots().map(agg -> (DomainType) agg).toList()) {
                        TypeStructure structure = domainType.structure();

                        // Skip interfaces as they don't have method implementations
                        if (structure.isInterface()) {
                            continue;
                        }

                        for (Method method : structure.methods()) {
                            totalMethods++;
                            totalComplexity += method.cyclomaticComplexity().orElse(1);
                        }
                    }

                    // Add entities
                    for (DomainType domainType :
                            domain.entities().map(entity -> (DomainType) entity).toList()) {
                        TypeStructure structure = domainType.structure();

                        if (structure.isInterface()) {
                            continue;
                        }

                        for (Method method : structure.methods()) {
                            totalMethods++;
                            totalComplexity += method.cyclomaticComplexity().orElse(1);
                        }
                    }

                    // Add value objects
                    for (DomainType domainType :
                            domain.valueObjects().map(vo -> (DomainType) vo).toList()) {
                        TypeStructure structure = domainType.structure();

                        if (structure.isInterface()) {
                            continue;
                        }

                        for (Method method : structure.methods()) {
                            totalMethods++;
                            totalComplexity += method.cyclomaticComplexity().orElse(1);
                        }
                    }

                    // Add domain services
                    for (DomainType domainType :
                            domain.domainServices().map(svc -> (DomainType) svc).toList()) {
                        TypeStructure structure = domainType.structure();

                        if (structure.isInterface()) {
                            continue;
                        }

                        for (Method method : structure.methods()) {
                            totalMethods++;
                            totalComplexity += method.cyclomaticComplexity().orElse(1);
                        }
                    }

                    if (totalMethods == 0) {
                        return Metric.of(
                                METRIC_NAME,
                                0.0,
                                "complexity",
                                "Average cyclomatic complexity for domain methods (no methods found)");
                    }

                    double averageComplexity = (double) totalComplexity / totalMethods;

                    return Metric.of(
                            METRIC_NAME,
                            averageComplexity,
                            "complexity",
                            "Average cyclomatic complexity for domain methods",
                            MetricThreshold.greaterThan(WARNING_THRESHOLD));
                })
                .orElse(Metric.of(
                        METRIC_NAME,
                        0.0,
                        "complexity",
                        "Average cyclomatic complexity for domain methods (domain index not available)"));
    }
}
