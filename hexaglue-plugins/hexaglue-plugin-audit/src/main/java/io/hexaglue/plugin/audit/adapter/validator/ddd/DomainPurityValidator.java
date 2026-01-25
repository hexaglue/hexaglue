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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DomainType;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DependencyEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that domain types do not import infrastructure dependencies.
 *
 * <p>DDD Principle: The domain layer should be pure and independent of infrastructure
 * concerns. Domain types should not depend on frameworks, databases, external APIs,
 * or any infrastructure implementation details. This ensures the domain model remains
 * portable, testable, and focused on business logic.
 *
 * <p>This validator detects forbidden infrastructure imports:
 * <ul>
 *   <li>JPA/Persistence APIs (javax.persistence.*, jakarta.persistence.*)</li>
 *   <li>Spring Framework (org.springframework.*)</li>
 *   <li>Hibernate (org.hibernate.*)</li>
 *   <li>Jackson JSON (com.fasterxml.jackson.*)</li>
 *   <li>JDBC (javax.sql.*, java.sql.*)</li>
 *   <li>AWS SDK (software.amazon.*)</li>
 *   <li>Stripe API (com.stripe.*)</li>
 *   <li>Any other infrastructure framework or SDK</li>
 * </ul>
 *
 * <p><strong>Constraint:</strong> ddd:domain-purity<br>
 * <strong>Severity:</strong> CRITICAL<br>
 * <strong>Rationale:</strong> Infrastructure dependencies in the domain layer create
 * tight coupling, making the domain model harder to test, understand, and evolve
 * independently. The domain should express business concepts in pure terms.
 *
 * @since 1.0.0
 */
public class DomainPurityValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("ddd:domain-purity");

    /**
     * Set of forbidden import prefixes that indicate infrastructure dependencies.
     *
     * <p>Domain types should not import any of these packages as they represent
     * infrastructure concerns rather than business logic.
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
    public ConstraintId constraintId() {
        return CONSTRAINT_ID;
    }

    /**
     * Validates domain purity using the v5 ArchitecturalModel API.
     *
     * @param model the architectural model containing domain types
     * @param codebase the codebase for dependency analysis
     * @param query the architecture query (not used in v5)
     * @return list of violations
     * @since 5.0.0
     */
    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Check if type registry is available
        if (model.typeRegistry().isEmpty()) {
            return violations; // Cannot validate without type registry
        }

        var typeRegistry = model.typeRegistry().get();

        // Find all domain layer types using v5 API
        List<DomainType> domainTypes = typeRegistry.all(DomainType.class).toList();

        for (DomainType domainType : domainTypes) {
            String domainTypeQName = domainType.id().qualifiedName();

            // Check dependencies for forbidden infrastructure imports
            Set<String> dependencies = codebase.dependencies().get(domainTypeQName);

            if (dependencies == null || dependencies.isEmpty()) {
                continue; // No dependencies to check
            }

            // Find all forbidden dependencies
            List<String> forbiddenDependencies = findForbiddenDependencies(dependencies);

            if (!forbiddenDependencies.isEmpty()) {
                Violation.Builder builder = Violation.builder(CONSTRAINT_ID)
                        .severity(Severity.CRITICAL)
                        .message("Domain type '%s' has %d forbidden infrastructure import(s)"
                                .formatted(domainType.id().simpleName(), forbiddenDependencies.size()))
                        .affectedType(domainTypeQName)
                        .location(SourceLocation.of(domainTypeQName, 1, 1));

                // Add evidence for each forbidden dependency
                for (String forbiddenDep : forbiddenDependencies) {
                    String category = categorizeDependency(forbiddenDep);
                    builder.evidence(DependencyEvidence.of(
                            "Forbidden %s dependency: %s".formatted(category, forbiddenDep),
                            domainTypeQName,
                            forbiddenDep));
                }

                violations.add(builder.build());
            }
        }

        return violations;
    }

    /**
     * Finds all dependencies that match forbidden infrastructure prefixes.
     *
     * @param dependencies the set of dependencies to check
     * @return list of forbidden dependencies
     */
    private List<String> findForbiddenDependencies(Set<String> dependencies) {
        return dependencies.stream().filter(this::isForbiddenDependency).toList();
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

    /**
     * Categorizes a forbidden dependency for better error messages.
     *
     * <p>This helps users understand why a dependency is forbidden.
     *
     * @param dependency the forbidden dependency
     * @return a human-readable category
     */
    private String categorizeDependency(String dependency) {
        if (dependency.startsWith("javax.persistence") || dependency.startsWith("jakarta.persistence")) {
            return "JPA/Persistence";
        } else if (dependency.startsWith("org.springframework")) {
            return "Spring Framework";
        } else if (dependency.startsWith("org.hibernate")) {
            return "Hibernate ORM";
        } else if (dependency.startsWith("com.fasterxml.jackson")) {
            return "Jackson JSON";
        } else if (dependency.startsWith("javax.sql") || dependency.startsWith("java.sql")) {
            return "JDBC";
        } else if (dependency.startsWith("software.amazon")) {
            return "AWS SDK";
        } else if (dependency.startsWith("com.stripe")) {
            return "Stripe API";
        } else if (dependency.startsWith("com.azure")) {
            return "Azure SDK";
        } else if (dependency.startsWith("com.google.cloud")) {
            return "Google Cloud SDK";
        } else if (dependency.startsWith("org.apache.kafka")
                || dependency.startsWith("com.rabbitmq")
                || dependency.startsWith("javax.jms")
                || dependency.startsWith("jakarta.jms")) {
            return "Messaging";
        } else if (dependency.startsWith("javax.servlet")
                || dependency.startsWith("jakarta.servlet")
                || dependency.startsWith("javax.ws.rs")
                || dependency.startsWith("jakarta.ws.rs")) {
            return "Web/HTTP";
        } else if (dependency.startsWith("javax.validation")
                || dependency.startsWith("jakarta.validation")
                || dependency.startsWith("org.hibernate.validator")) {
            return "Validation Framework";
        } else {
            return "infrastructure";
        }
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }
}
