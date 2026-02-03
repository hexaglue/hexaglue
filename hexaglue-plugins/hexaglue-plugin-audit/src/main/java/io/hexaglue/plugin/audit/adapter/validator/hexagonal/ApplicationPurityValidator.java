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

package io.hexaglue.plugin.audit.adapter.validator.hexagonal;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.ApplicationType;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DependencyEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.ConstraintValidator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates that application layer types do not import infrastructure dependencies.
 *
 * <p>Hexagonal Architecture Principle: The application layer (use cases, application services)
 * should be framework-agnostic. Application services orchestrate domain logic through ports
 * and should not depend on frameworks, databases, external APIs, or any infrastructure
 * implementation details. This ensures the application core remains portable and testable
 * independently of any framework.
 *
 * <p>This validator uses a two-pronged detection strategy:
 * <ol>
 *   <li><strong>Classified types</strong>: Checks all {@link ApplicationType} instances
 *       from the type registry</li>
 *   <li><strong>Unclassified/excluded types</strong>: Scans {@code codebase.dependencies()}
 *       for types whose package contains {@code .application.} that are not already in
 *       the registry — this handles scenarios where application services are excluded
 *       from classification but still visible in the codebase</li>
 * </ol>
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
 * <p><strong>Constraint:</strong> hexagonal:application-purity<br>
 * <strong>Severity:</strong> MAJOR<br>
 * <strong>Rationale:</strong> Infrastructure dependencies in the application layer create
 * tight coupling, making the application services harder to test, understand, and evolve
 * independently. The application layer should express use case orchestration in pure terms.
 *
 * @since 5.0.0
 */
public class ApplicationPurityValidator implements ConstraintValidator {

    private static final ConstraintId CONSTRAINT_ID = ConstraintId.of("hexagonal:application-purity");

    /**
     * Set of forbidden import prefixes that indicate infrastructure dependencies.
     *
     * <p>Application types should not import any of these packages as they represent
     * infrastructure concerns rather than application orchestration logic.
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
     * Validates application layer purity using a two-pronged detection strategy.
     *
     * <p>First checks classified {@link ApplicationType} instances from the type registry,
     * then scans codebase dependencies for unclassified types in {@code .application.} packages.
     *
     * @param model the architectural model containing application types
     * @param codebase the codebase for dependency analysis
     * @param query the architecture query (not used)
     * @return list of violations
     * @since 5.0.0
     */
    @Override
    public List<Violation> validate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
        List<Violation> violations = new ArrayList<>();

        // Collect qualified names of classified ApplicationType instances
        Set<String> classifiedAppTypes = new HashSet<>();

        if (model.typeRegistry().isPresent()) {
            var typeRegistry = model.typeRegistry().get();

            List<ApplicationType> appTypes =
                    typeRegistry.all(ApplicationType.class).toList();

            for (ApplicationType appType : appTypes) {
                String appTypeQName = appType.id().qualifiedName();
                classifiedAppTypes.add(appTypeQName);

                checkDependencies(appTypeQName, appType.id().simpleName(), codebase, violations);
            }
        }

        // Scan codebase dependencies for unclassified types in .application. packages
        for (String typeQName : codebase.dependencies().keySet()) {
            if (classifiedAppTypes.contains(typeQName)) {
                continue; // Already checked above
            }

            if (isApplicationPackageType(typeQName)) {
                String simpleName = extractSimpleName(typeQName);
                checkDependencies(typeQName, simpleName, codebase, violations);
            }
        }

        return violations;
    }

    /**
     * Checks whether a type's qualified name indicates it belongs to an application package.
     *
     * @param qualifiedName the fully qualified type name
     * @return true if the type is in an application package
     */
    private boolean isApplicationPackageType(String qualifiedName) {
        return qualifiedName.contains(".application.");
    }

    /**
     * Extracts the simple class name from a fully qualified name.
     *
     * @param qualifiedName the fully qualified type name
     * @return the simple class name
     */
    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * Checks dependencies for a given type and adds violations if forbidden imports are found.
     *
     * @param qualifiedName the fully qualified type name
     * @param simpleName the simple type name (for messages)
     * @param codebase the codebase for dependency lookup
     * @param violations the list to add violations to
     */
    private void checkDependencies(
            String qualifiedName, String simpleName, Codebase codebase, List<Violation> violations) {
        Set<String> dependencies = codebase.dependencies().get(qualifiedName);

        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        List<String> forbiddenDependencies = findForbiddenDependencies(dependencies);

        if (!forbiddenDependencies.isEmpty()) {
            Violation.Builder builder = Violation.builder(CONSTRAINT_ID)
                    .severity(Severity.MAJOR)
                    .message("Application type '%s' has %d forbidden infrastructure import(s)"
                            .formatted(simpleName, forbiddenDependencies.size()))
                    .affectedType(qualifiedName)
                    .location(SourceLocation.of(qualifiedName, 1, 1));

            for (String forbiddenDep : forbiddenDependencies) {
                String category = categorizeDependency(forbiddenDep);
                builder.evidence(DependencyEvidence.of(
                        "Forbidden %s dependency: %s".formatted(category, forbiddenDep), qualifiedName, forbiddenDep));
            }

            violations.add(builder.build());
        }
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
     * Checks if a dependency is forbidden for application layer types.
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
        return Severity.MAJOR;
    }
}
