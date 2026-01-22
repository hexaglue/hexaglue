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
import io.hexaglue.arch.model.MethodRole;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;

/**
 * Calculates the ratio of boilerplate code to total code in domain types.
 *
 * <p>This calculator estimates the percentage of methods that are boilerplate
 * (getters, setters, equals, hashCode, toString, constructors, and builder patterns)
 * versus domain logic. A high boilerplate ratio may indicate opportunities for
 * using records, Lombok, or other code generation tools.
 *
 * <p><strong>Metric:</strong> code.boilerplate.ratio<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if > 50%<br>
 * <strong>Interpretation:</strong> Lower is better. High boilerplate ratio suggests
 * the domain model might benefit from modern Java features (records) or code
 * generation tools. It may also indicate an anemic domain model if most code is
 * data access rather than business logic.
 *
 * <h2>Boilerplate Detection</h2>
 * <p>Methods are classified as boilerplate based on their {@link MethodRole}:
 * <ul>
 *   <li><strong>Getters:</strong> Methods with {@link MethodRole#GETTER}</li>
 *   <li><strong>Setters:</strong> Methods with {@link MethodRole#SETTER}</li>
 *   <li><strong>Infrastructure:</strong> {@link MethodRole#OBJECT_METHOD}, {@link MethodRole#FACTORY}, {@link MethodRole#LIFECYCLE}</li>
 * </ul>
 *
 * <h2>Scope</h2>
 * <p>This metric only analyzes domain layer types (aggregate roots, entities, value
 * objects, domain services) as boilerplate in other layers serves different purposes
 * and is often necessary.
 *
 * @since 1.0.0
 */
public class BoilerplateMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "code.boilerplate.ratio";
    private static final double WARNING_THRESHOLD = 50.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the percentage of boilerplate code using the v5 ArchType API.
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
                    int boilerplateMethods = 0;

                    // Process all domain types
                    for (DomainType domainType :
                            domain.aggregateRoots().map(agg -> (DomainType) agg).toList()) {
                        TypeStructure structure = domainType.structure();

                        // Skip interfaces and records as they don't have boilerplate implementations
                        if (structure.isInterface() || structure.isRecord()) {
                            continue;
                        }

                        for (Method method : structure.methods()) {
                            totalMethods++;
                            if (isBoilerplate(method)) {
                                boilerplateMethods++;
                            }
                        }
                    }

                    // Add entities
                    for (DomainType domainType :
                            domain.entities().map(entity -> (DomainType) entity).toList()) {
                        TypeStructure structure = domainType.structure();

                        if (structure.isInterface() || structure.isRecord()) {
                            continue;
                        }

                        for (Method method : structure.methods()) {
                            totalMethods++;
                            if (isBoilerplate(method)) {
                                boilerplateMethods++;
                            }
                        }
                    }

                    // Add value objects
                    for (DomainType domainType :
                            domain.valueObjects().map(vo -> (DomainType) vo).toList()) {
                        TypeStructure structure = domainType.structure();

                        if (structure.isInterface() || structure.isRecord()) {
                            continue;
                        }

                        for (Method method : structure.methods()) {
                            totalMethods++;
                            if (isBoilerplate(method)) {
                                boilerplateMethods++;
                            }
                        }
                    }

                    // Add domain services
                    for (DomainType domainType :
                            domain.domainServices().map(svc -> (DomainType) svc).toList()) {
                        TypeStructure structure = domainType.structure();

                        if (structure.isInterface() || structure.isRecord()) {
                            continue;
                        }

                        for (Method method : structure.methods()) {
                            totalMethods++;
                            if (isBoilerplate(method)) {
                                boilerplateMethods++;
                            }
                        }
                    }

                    if (totalMethods == 0) {
                        return Metric.of(
                                METRIC_NAME,
                                0.0,
                                "%",
                                "Percentage of boilerplate code in domain types (no methods found)");
                    }

                    double ratio = (double) boilerplateMethods / totalMethods * 100.0;

                    return Metric.of(
                            METRIC_NAME,
                            ratio,
                            "%",
                            "Percentage of boilerplate code in domain types",
                            MetricThreshold.greaterThan(WARNING_THRESHOLD));
                })
                .orElse(Metric.of(
                        METRIC_NAME,
                        0.0,
                        "%",
                        "Percentage of boilerplate code in domain types (domain index not available)"));
    }

    /**
     * Determines if a method is boilerplate code based on its roles.
     *
     * <p>A method is considered boilerplate if it has any of these roles:
     * <ul>
     *   <li>{@link MethodRole#GETTER}</li>
     *   <li>{@link MethodRole#SETTER}</li>
     *   <li>{@link MethodRole#OBJECT_METHOD} (equals, hashCode, toString)</li>
     *   <li>{@link MethodRole#FACTORY} (factory methods)</li>
     *   <li>{@link MethodRole#LIFECYCLE} (lifecycle callbacks)</li>
     * </ul>
     *
     * @param method the method to check
     * @return true if the method is boilerplate
     */
    private boolean isBoilerplate(Method method) {
        return method.roles().contains(MethodRole.GETTER)
                || method.roles().contains(MethodRole.SETTER)
                || method.roles().contains(MethodRole.OBJECT_METHOD)
                || method.roles().contains(MethodRole.FACTORY)
                || method.roles().contains(MethodRole.LIFECYCLE);
    }
}
