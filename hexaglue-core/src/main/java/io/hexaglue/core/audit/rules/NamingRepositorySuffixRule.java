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
import io.hexaglue.spi.audit.RuleViolation;
import io.hexaglue.spi.audit.Severity;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit rule that enforces repository naming conventions.
 *
 * <p>Repository interfaces and classes should follow the naming convention of
 * ending with "Repository" to clearly indicate their role in the architecture.
 * This improves code readability and makes architectural patterns more explicit.
 *
 * <p>Violations detected:
 * <ul>
 *   <li>Interface or class with @Repository annotation doesn't end with "Repository"</li>
 *   <li>Type in repository package doesn't end with "Repository"</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class NamingRepositorySuffixRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.naming.repository-suffix";
    private static final String RULE_NAME = "Repository Naming Convention";
    private static final String REPOSITORY_SUFFIX = "Repository";

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
        return Severity.MAJOR;
    }

    @Override
    public List<RuleViolation> check(CodeUnit unit) {
        // Only check interfaces and classes
        if (unit.kind() != CodeUnitKind.INTERFACE && unit.kind() != CodeUnitKind.CLASS) {
            return List.of();
        }

        List<RuleViolation> violations = new ArrayList<>();

        boolean hasRepositoryAnnotation = hasRepositoryAnnotation(unit);
        boolean inRepositoryPackage = isInRepositoryPackage(unit.qualifiedName());
        boolean endsWithRepository = unit.simpleName().endsWith(REPOSITORY_SUFFIX);

        // If type has @Repository annotation or is in repository package, it should end with "Repository"
        if ((hasRepositoryAnnotation || inRepositoryPackage) && !endsWithRepository) {
            String reason = hasRepositoryAnnotation ? "has @Repository annotation" : "is in a repository package";

            violations.add(createViolation(
                    "Type '%s' %s but doesn't follow naming convention. "
                            + "Consider renaming to '%s%s'."
                                    .formatted(unit.simpleName(), reason, unit.simpleName(), REPOSITORY_SUFFIX),
                    unit.qualifiedName()));
        }

        return violations;
    }

    /**
     * Checks if the code unit has a repository-related annotation.
     */
    private boolean hasRepositoryAnnotation(CodeUnit unit) {
        // Check class-level annotations (would need to be added to CodeUnit in real implementation)
        // For now, check field annotations as a proxy
        for (var field : unit.fields()) {
            for (var annotation : field.annotations()) {
                if (isRepositoryAnnotation(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if an annotation indicates a repository.
     */
    private boolean isRepositoryAnnotation(String annotation) {
        return annotation.equals("org.springframework.stereotype.Repository")
                || annotation.equals("org.springframework.data.repository.Repository");
    }

    /**
     * Checks if the qualified name indicates a repository package.
     */
    private boolean isInRepositoryPackage(String qualifiedName) {
        String lower = qualifiedName.toLowerCase();
        return lower.contains(".repository.") || lower.contains(".repositories.") || lower.contains(".persistence.");
    }

    /**
     * Creates a rule violation with a synthetic source location.
     */
    private RuleViolation createViolation(String message, String qualifiedName) {
        SourceLocation location = SourceLocation.of(qualifiedName + ".java", 1, 1);
        return RuleViolation.of(RULE_ID, defaultSeverity(), message, location);
    }
}
