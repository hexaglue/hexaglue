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
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RuleViolation;
import io.hexaglue.spi.audit.Severity;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit rule that enforces controller naming conventions.
 *
 * <p>Controller classes (REST, GraphQL, etc.) should follow the naming convention
 * of ending with "Controller" to clearly indicate their role as presentation layer
 * components. This improves code readability and makes architectural patterns
 * more explicit.
 *
 * <p>Violations detected:
 * <ul>
 *   <li>Class with @Controller or @RestController annotation doesn't end with "Controller"</li>
 *   <li>Presentation layer class in controller package doesn't end with "Controller"</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class NamingControllerSuffixRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.naming.controller-suffix";
    private static final String RULE_NAME = "Controller Naming Convention";
    private static final String CONTROLLER_SUFFIX = "Controller";

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
        // Only check classes
        if (unit.kind() != CodeUnitKind.CLASS) {
            return List.of();
        }

        List<RuleViolation> violations = new ArrayList<>();

        boolean hasControllerAnnotation = hasControllerAnnotation(unit);
        boolean inControllerPackage =
                isInControllerPackage(unit.qualifiedName()) && unit.layer() == LayerClassification.PRESENTATION;
        boolean endsWithController = unit.simpleName().endsWith(CONTROLLER_SUFFIX);

        // If class has @Controller annotation or is in controller package, it should end with "Controller"
        if ((hasControllerAnnotation || inControllerPackage) && !endsWithController) {
            String reason = hasControllerAnnotation
                    ? "has @Controller or @RestController annotation"
                    : "is in a controller package";

            violations.add(createViolation(
                    "Type '%s' %s but doesn't follow naming convention. "
                            + "Consider renaming to '%s%s'."
                                    .formatted(unit.simpleName(), reason, unit.simpleName(), CONTROLLER_SUFFIX),
                    unit.qualifiedName()));
        }

        return violations;
    }

    /**
     * Checks if the code unit has a controller-related annotation.
     */
    private boolean hasControllerAnnotation(CodeUnit unit) {
        // Check method annotations as a proxy for class-level annotations
        for (var method : unit.methods()) {
            for (var annotation : method.annotations()) {
                if (isControllerAnnotation(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if an annotation indicates a controller.
     */
    private boolean isControllerAnnotation(String annotation) {
        return annotation.equals("org.springframework.stereotype.Controller")
                || annotation.equals("org.springframework.web.bind.annotation.RestController")
                || annotation.equals("org.springframework.web.bind.annotation.ControllerAdvice")
                || annotation.equals("jakarta.ws.rs.Path")
                || annotation.equals("javax.ws.rs.Path");
    }

    /**
     * Checks if the qualified name indicates a controller package.
     */
    private boolean isInControllerPackage(String qualifiedName) {
        String lower = qualifiedName.toLowerCase();
        return lower.contains(".controller.")
                || lower.contains(".controllers.")
                || lower.contains(".rest.")
                || lower.contains(".api.")
                || lower.contains(".web.");
    }

    /**
     * Creates a rule violation with a synthetic source location.
     */
    private RuleViolation createViolation(String message, String qualifiedName) {
        SourceLocation location = SourceLocation.of(qualifiedName + ".java", 1, 1);
        return new RuleViolation(RULE_ID, defaultSeverity(), message, location);
    }
}
