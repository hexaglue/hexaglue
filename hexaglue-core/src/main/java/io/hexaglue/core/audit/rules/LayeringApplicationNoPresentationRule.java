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

import io.hexaglue.arch.model.audit.CodeUnit;
import io.hexaglue.arch.model.audit.LayerClassification;
import io.hexaglue.arch.model.audit.RuleViolation;
import io.hexaglue.arch.model.audit.Severity;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.spi.audit.AuditRule;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit rule that prevents application layer from depending on presentation layer.
 *
 * <p>The application layer should be independent of the presentation layer to
 * allow the same use cases to be exposed through different interfaces (REST,
 * GraphQL, CLI, etc.) without modification. This follows the Dependency Inversion
 * Principle.
 *
 * <p>Violations detected:
 * <ul>
 *   <li>Application class has field of presentation type</li>
 *   <li>Application method has parameter or return type from presentation</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class LayeringApplicationNoPresentationRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.layer.application-no-presentation";
    private static final String RULE_NAME = "Application Should Not Depend on Presentation";

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
        return Severity.CRITICAL;
    }

    @Override
    public List<RuleViolation> check(CodeUnit unit) {
        // Only check application layer types
        if (unit.layer() != LayerClassification.APPLICATION) {
            return List.of();
        }

        List<RuleViolation> violations = new ArrayList<>();

        // Check field types for presentation dependencies
        for (var field : unit.fields()) {
            if (isPresentationType(field.type())) {
                violations.add(createViolation(
                        "Application type '%s' has field '%s' of presentation type '%s'. "
                                + "Application layer must not depend on presentation layer."
                                        .formatted(unit.simpleName(), field.name(), extractSimpleName(field.type())),
                        unit.qualifiedName()));
            }

            // Check for presentation annotations on fields
            for (var annotation : field.annotations()) {
                if (isPresentationAnnotation(annotation)) {
                    violations.add(createViolation(
                            "Application type '%s' field '%s' has presentation annotation '@%s'"
                                    .formatted(unit.simpleName(), field.name(), extractSimpleName(annotation)),
                            unit.qualifiedName()));
                }
            }
        }

        // Check method signatures for presentation dependencies
        for (var method : unit.methods()) {
            // Check return type
            if (isPresentationType(method.returnType())) {
                violations.add(createViolation(
                        "Application type '%s' method '%s' returns presentation type '%s'. "
                                + "Application layer must not depend on presentation layer."
                                        .formatted(
                                                unit.simpleName(),
                                                method.name(),
                                                extractSimpleName(method.returnType())),
                        unit.qualifiedName()));
            }

            // Check parameter types
            for (var paramType : method.parameterTypes()) {
                if (isPresentationType(paramType)) {
                    violations.add(createViolation(
                            "Application type '%s' method '%s' has parameter of presentation type '%s'. "
                                    + "Application layer must not depend on presentation layer."
                                            .formatted(unit.simpleName(), method.name(), extractSimpleName(paramType)),
                            unit.qualifiedName()));
                }
            }

            // Check method annotations
            for (var annotation : method.annotations()) {
                if (isPresentationAnnotation(annotation)) {
                    violations.add(createViolation(
                            "Application type '%s' method '%s' has presentation annotation '@%s'"
                                    .formatted(unit.simpleName(), method.name(), extractSimpleName(annotation)),
                            unit.qualifiedName()));
                }
            }
        }

        return violations;
    }

    /**
     * Checks if a type name indicates a presentation type.
     * Heuristic: checks for common presentation packages.
     */
    private boolean isPresentationType(String qualifiedTypeName) {
        if (qualifiedTypeName == null) {
            return false;
        }

        String lower = qualifiedTypeName.toLowerCase();

        // Check for common presentation package indicators
        return lower.contains(".presentation.")
                || lower.contains(".ui.")
                || lower.contains(".web.")
                || lower.contains(".rest.")
                || lower.contains(".api.")
                || lower.contains(".controller.")
                || lower.contains(".graphql.")
                || lower.contains(".grpc.")
                // Spring MVC
                || qualifiedTypeName.startsWith("org.springframework.web.")
                || qualifiedTypeName.startsWith("org.springframework.http.")
                // JAX-RS
                || qualifiedTypeName.startsWith("jakarta.ws.rs.")
                || qualifiedTypeName.startsWith("javax.ws.rs.");
    }

    /**
     * Checks if an annotation indicates presentation concern.
     */
    private boolean isPresentationAnnotation(String qualifiedAnnotationName) {
        if (qualifiedAnnotationName == null) {
            return false;
        }

        return qualifiedAnnotationName.equals("org.springframework.stereotype.Controller")
                || qualifiedAnnotationName.equals("org.springframework.web.bind.annotation.RestController")
                || qualifiedAnnotationName.equals("org.springframework.web.bind.annotation.ControllerAdvice")
                || qualifiedAnnotationName.startsWith("org.springframework.web.bind.annotation.")
                || qualifiedAnnotationName.equals("jakarta.ws.rs.Path")
                || qualifiedAnnotationName.equals("javax.ws.rs.Path")
                || qualifiedAnnotationName.startsWith("jakarta.ws.rs.")
                || qualifiedAnnotationName.startsWith("javax.ws.rs.");
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
        SourceLocation location = SourceLocation.of(qualifiedName + ".java", 1, 1);
        return RuleViolation.of(RULE_ID, defaultSeverity(), message, location);
    }
}
