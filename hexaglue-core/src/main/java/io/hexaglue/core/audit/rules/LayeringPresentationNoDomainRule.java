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
 * Audit rule that prevents presentation layer from directly accessing domain layer.
 *
 * <p>In a properly layered architecture, the presentation layer should interact
 * with the application layer (use cases), not directly with the domain layer.
 * This ensures proper separation of concerns and makes the system easier to test
 * and evolve.
 *
 * <p>Violations detected:
 * <ul>
 *   <li>Presentation class has field of domain type</li>
 *   <li>Presentation method has parameter or return type from domain</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class LayeringPresentationNoDomainRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.layer.presentation-no-domain";
    private static final String RULE_NAME = "Presentation Should Not Access Domain Directly";

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
        return Severity.WARNING;
    }

    @Override
    public List<RuleViolation> check(CodeUnit unit) {
        // Only check presentation layer types
        if (unit.layer() != LayerClassification.PRESENTATION) {
            return List.of();
        }

        List<RuleViolation> violations = new ArrayList<>();

        // Check field types for domain dependencies
        for (var field : unit.fields()) {
            if (isDomainType(field.type())) {
                violations.add(createViolation(
                        "Presentation type '%s' has field '%s' of domain type '%s'. "
                                + "Consider using application layer services instead."
                                        .formatted(unit.simpleName(), field.name(), extractSimpleName(field.type())),
                        unit.qualifiedName()));
            }
        }

        // Check method signatures for domain dependencies
        for (var method : unit.methods()) {
            // Check return type
            if (isDomainType(method.returnType())) {
                violations.add(createViolation(
                        "Presentation type '%s' method '%s' returns domain type '%s'. "
                                + "Consider using DTOs or view models."
                                        .formatted(
                                                unit.simpleName(),
                                                method.name(),
                                                extractSimpleName(method.returnType())),
                        unit.qualifiedName()));
            }

            // Check parameter types
            for (var paramType : method.parameterTypes()) {
                if (isDomainType(paramType)) {
                    violations.add(createViolation(
                            "Presentation type '%s' method '%s' has parameter of domain type '%s'. "
                                    + "Consider using DTOs or commands."
                                            .formatted(unit.simpleName(), method.name(), extractSimpleName(paramType)),
                            unit.qualifiedName()));
                }
            }
        }

        return violations;
    }

    /**
     * Checks if a type name indicates a domain type.
     * Heuristic: checks for common domain packages.
     */
    private boolean isDomainType(String qualifiedTypeName) {
        if (qualifiedTypeName == null) {
            return false;
        }

        String lower = qualifiedTypeName.toLowerCase();

        // Check for common domain package indicators
        return lower.contains(".domain.")
                || lower.contains(".model.")
                || lower.contains(".entity.")
                || lower.contains(".valueobject.")
                || lower.contains(".aggregate.");
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
        return new RuleViolation(RULE_ID, defaultSeverity(), message, location);
    }
}
