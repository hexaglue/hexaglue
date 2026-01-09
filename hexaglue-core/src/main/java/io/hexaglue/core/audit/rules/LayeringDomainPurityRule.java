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

package io.hexaglue.core.audit.rules;

import io.hexaglue.spi.audit.AuditRule;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RuleViolation;
import io.hexaglue.spi.audit.Severity;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit rule that enforces domain layer purity.
 *
 * <p>The domain layer should not depend on infrastructure layer types.
 * This rule detects violations where domain classes have dependencies on
 * infrastructure classes, which would create unwanted coupling and violate
 * the Dependency Inversion Principle.
 *
 * <p>Violations detected:
 * <ul>
 *   <li>Domain class has field of infrastructure type</li>
 *   <li>Domain method has parameter or return type from infrastructure</li>
 *   <li>Domain class is annotated with infrastructure annotations</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class LayeringDomainPurityRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.layer.domain-purity";
    private static final String RULE_NAME = "Domain Layer Purity";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public String name() {
        return RULE_NAME;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.ERROR;
    }

    @Override
    public List<RuleViolation> check(CodeUnit unit) {
        // Only check domain layer types
        if (unit.layer() != LayerClassification.DOMAIN) {
            return List.of();
        }

        List<RuleViolation> violations = new ArrayList<>();

        // Check field types for infrastructure dependencies
        for (var field : unit.fields()) {
            if (isInfrastructureType(field.type())) {
                violations.add(createViolation(
                        "Domain type '%s' has field '%s' of infrastructure type '%s'"
                                .formatted(unit.simpleName(), field.name(), extractSimpleName(field.type())),
                        unit.qualifiedName()));
            }

            // Check field annotations
            for (var annotation : field.annotations()) {
                if (isInfrastructureAnnotation(annotation)) {
                    violations.add(createViolation(
                            "Domain type '%s' field '%s' has infrastructure annotation '@%s'"
                                    .formatted(unit.simpleName(), field.name(), extractSimpleName(annotation)),
                            unit.qualifiedName()));
                }
            }
        }

        // Check method signatures for infrastructure dependencies
        for (var method : unit.methods()) {
            // Check return type
            if (isInfrastructureType(method.returnType())) {
                violations.add(createViolation(
                        "Domain type '%s' method '%s' returns infrastructure type '%s'"
                                .formatted(unit.simpleName(), method.name(), extractSimpleName(method.returnType())),
                        unit.qualifiedName()));
            }

            // Check parameter types
            for (var paramType : method.parameterTypes()) {
                if (isInfrastructureType(paramType)) {
                    violations.add(createViolation(
                            "Domain type '%s' method '%s' has parameter of infrastructure type '%s'"
                                    .formatted(unit.simpleName(), method.name(), extractSimpleName(paramType)),
                            unit.qualifiedName()));
                }
            }

            // Check method annotations
            for (var annotation : method.annotations()) {
                if (isInfrastructureAnnotation(annotation)) {
                    violations.add(createViolation(
                            "Domain type '%s' method '%s' has infrastructure annotation '@%s'"
                                    .formatted(unit.simpleName(), method.name(), extractSimpleName(annotation)),
                            unit.qualifiedName()));
                }
            }
        }

        return violations;
    }

    /**
     * Checks if a type name indicates an infrastructure type.
     * Heuristic: checks for common infrastructure packages.
     */
    private boolean isInfrastructureType(String qualifiedTypeName) {
        if (qualifiedTypeName == null) {
            return false;
        }

        String lower = qualifiedTypeName.toLowerCase();

        // Check for common infrastructure package indicators
        return lower.contains(".infrastructure.")
                || lower.contains(".persistence.")
                || lower.contains(".repository.")
                || lower.contains(".adapter.")
                || lower.contains(".messaging.")
                || lower.contains(".external.")
                // Spring Data / JPA
                || qualifiedTypeName.startsWith("org.springframework.data.")
                || qualifiedTypeName.startsWith("jakarta.persistence.")
                || qualifiedTypeName.startsWith("javax.persistence.")
                // Messaging
                || qualifiedTypeName.startsWith("org.springframework.jms.")
                || qualifiedTypeName.startsWith("org.springframework.amqp.");
    }

    /**
     * Checks if an annotation indicates infrastructure concern.
     */
    private boolean isInfrastructureAnnotation(String qualifiedAnnotationName) {
        if (qualifiedAnnotationName == null) {
            return false;
        }

        return qualifiedAnnotationName.startsWith("org.springframework.data.")
                || qualifiedAnnotationName.startsWith("jakarta.persistence.")
                || qualifiedAnnotationName.startsWith("javax.persistence.")
                || qualifiedAnnotationName.equals("org.springframework.stereotype.Repository")
                || qualifiedAnnotationName.equals("org.springframework.data.repository.Repository");
    }

    /**
     * Extracts the simple name from a qualified name.
     */
    private String extractSimpleName(String qualifiedName) {
        if (qualifiedName == null) {
            return "";
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * Creates a rule violation with a synthetic source location.
     */
    private RuleViolation createViolation(String message, String qualifiedName) {
        // Create a synthetic location pointing to the type
        SourceLocation location = SourceLocation.of(qualifiedName + ".java", 1, 1);
        return new RuleViolation(RULE_ID, defaultSeverity(), message, location);
    }
}
