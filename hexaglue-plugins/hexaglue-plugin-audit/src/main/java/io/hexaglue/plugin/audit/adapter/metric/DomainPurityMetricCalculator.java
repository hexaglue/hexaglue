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
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Calculates domain purity metrics.
 *
 * <p>This calculator measures what percentage of domain types are free from
 * infrastructure dependencies. A healthy hexagonal architecture should have
 * domain types that are pure and independent of infrastructure concerns.
 *
 * <p>The metric considers a domain type "pure" if it has no dependencies on:
 * <ul>
 *   <li>JPA/Persistence APIs (javax.persistence.*, jakarta.persistence.*)</li>
 *   <li>Spring Framework (org.springframework.*)</li>
 *   <li>Hibernate (org.hibernate.*)</li>
 *   <li>Jackson JSON (com.fasterxml.jackson.*)</li>
 *   <li>JDBC (javax.sql.*, java.sql.*)</li>
 *   <li>Cloud/External SDKs (AWS, Azure, Google Cloud, Stripe, etc.)</li>
 *   <li>Messaging frameworks (Kafka, RabbitMQ, JMS)</li>
 *   <li>Web/HTTP frameworks (Servlet, JAX-RS)</li>
 *   <li>Validation frameworks (javax.validation.*, jakarta.validation.*)</li>
 * </ul>
 *
 * <p><strong>Metric:</strong> domain.purity<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if < 100%<br>
 * <strong>Interpretation:</strong> Should be 100%. Any infrastructure dependencies
 * in the domain layer indicate architectural violations that should be resolved
 * by moving infrastructure concerns to adapters.
 *
 * @since 1.0.0
 */
public class DomainPurityMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "domain.purity";
    private static final double EXPECTED_PURITY = 100.0;

    /**
     * Set of forbidden import prefixes that indicate infrastructure dependencies.
     *
     * <p>This list is aligned with {@link io.hexaglue.plugin.audit.adapter.validator.ddd.DomainPurityValidator}
     * to ensure consistency between validation and metrics.
     */
    private static final Set<String> FORBIDDEN_PREFIXES = Set.of(
            // JPA/Persistence
            "javax.persistence",
            "jakarta.persistence",

            // Spring Framework
            "org.springframework",

            // Hibernate
            "org.hibernate",

            // Jackson JSON
            "com.fasterxml.jackson",

            // JDBC
            "javax.sql",
            "java.sql",

            // Cloud/External SDKs
            "software.amazon", // AWS SDK
            "com.stripe", // Stripe API
            "com.azure", // Azure SDK
            "com.google.cloud", // Google Cloud SDK

            // Messaging/Integration
            "org.apache.kafka",
            "com.rabbitmq",
            "javax.jms",
            "jakarta.jms",

            // Web/HTTP
            "javax.servlet",
            "jakarta.servlet",
            "javax.ws.rs",
            "jakarta.ws.rs",

            // Validation (should use domain-specific validation)
            "javax.validation",
            "jakarta.validation",
            "org.hibernate.validator");

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the percentage of pure domain types using the v5 ArchType API.
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for dependency graph access
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.0.0
     */
    @Override
    public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery) {
        return model.domainIndex()
                .map(domain -> {
                    // Collect all domain types
                    List<DomainType> domainTypes = new ArrayList<>();
                    domain.aggregateRoots().forEach(domainTypes::add);
                    domain.entities().forEach(domainTypes::add);
                    domain.valueObjects().forEach(domainTypes::add);
                    domain.identifiers().forEach(domainTypes::add);
                    domain.domainEvents().forEach(domainTypes::add);
                    domain.domainServices().forEach(domainTypes::add);

                    if (domainTypes.isEmpty()) {
                        return Metric.of(
                                METRIC_NAME,
                                100.0,
                                "%",
                                "Percentage of domain types without infrastructure dependencies (no domain types found)");
                    }

                    // Count domain types that are pure (no infrastructure dependencies)
                    long pureTypes = domainTypes.stream()
                            .filter(domainType -> isPure(domainType, codebase))
                            .count();

                    double purity = (double) pureTypes / domainTypes.size() * 100.0;

                    return Metric.of(
                            METRIC_NAME,
                            purity,
                            "%",
                            "Percentage of domain types without infrastructure dependencies",
                            MetricThreshold.lessThan(EXPECTED_PURITY));
                })
                .orElse(Metric.of(
                        METRIC_NAME,
                        100.0,
                        "%",
                        "Percentage of domain types without infrastructure dependencies (domain index not available)"));
    }

    /**
     * Checks if a domain type is pure (free from infrastructure dependencies).
     *
     * <p>A domain type is considered pure if none of its dependencies match the
     * forbidden infrastructure prefixes.
     *
     * @param domainType the domain type to check
     * @param codebase   the codebase containing dependency information
     * @return true if the type has no infrastructure dependencies
     */
    private boolean isPure(DomainType domainType, Codebase codebase) {
        String qualifiedName = domainType.id().qualifiedName();
        Set<String> dependencies = codebase.dependencies().get(qualifiedName);

        if (dependencies == null || dependencies.isEmpty()) {
            return true; // No dependencies means pure
        }

        // Check if any dependency matches forbidden prefixes
        return dependencies.stream().noneMatch(this::isForbiddenDependency);
    }

    /**
     * Checks if a dependency is forbidden for domain layer types.
     *
     * @param dependency the fully qualified dependency name
     * @return true if the dependency is forbidden
     */
    private boolean isForbiddenDependency(String dependency) {
        return FORBIDDEN_PREFIXES.stream().anyMatch(dependency::startsWith);
    }
}
