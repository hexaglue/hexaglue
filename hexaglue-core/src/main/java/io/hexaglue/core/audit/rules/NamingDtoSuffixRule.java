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
 * Audit rule that enforces DTO naming conventions.
 *
 * <p>Data Transfer Objects (DTOs) should follow the naming convention of ending
 * with "Dto" or "DTO" to clearly distinguish them from domain entities. This
 * prevents confusion and makes the architectural boundaries more explicit.
 *
 * <p>Violations detected:
 * <ul>
 *   <li>Class or record in dto/transfer package doesn't end with "Dto" or "DTO"</li>
 *   <li>Presentation layer data types should follow DTO conventions</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class NamingDtoSuffixRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.naming.dto-suffix";
    private static final String RULE_NAME = "DTO Naming Convention";

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
        return Severity.INFO;
    }

    @Override
    public List<RuleViolation> check(CodeUnit unit) {
        // Only check classes and records
        if (unit.kind() != CodeUnitKind.CLASS && unit.kind() != CodeUnitKind.RECORD) {
            return List.of();
        }

        List<RuleViolation> violations = new ArrayList<>();

        boolean inDtoPackage = isInDtoPackage(unit.qualifiedName());
        boolean hasDtoSuffix =
                unit.simpleName().endsWith("Dto") || unit.simpleName().endsWith("DTO");

        // If type is in dto/transfer package, it should end with Dto/DTO
        if (inDtoPackage && !hasDtoSuffix) {
            violations.add(createViolation(
                    "Type '%s' is in a DTO/transfer package but doesn't follow naming convention. "
                            + "Consider renaming to '%sDto' or '%sDTO'."
                                    .formatted(unit.simpleName(), unit.simpleName(), unit.simpleName()),
                    unit.qualifiedName()));
        }

        // Additional check: presentation layer records should consider DTO suffix
        if (unit.kind() == CodeUnitKind.RECORD
                && unit.layer() == LayerClassification.PRESENTATION
                && !hasDtoSuffix
                && !unit.simpleName().endsWith("Request")
                && !unit.simpleName().endsWith("Response")
                && !unit.simpleName().endsWith("Command")
                && !unit.simpleName().endsWith("Query")) {

            violations.add(createViolation(
                    "Presentation layer record '%s' might be a DTO. "
                            + "Consider adding 'Dto' suffix for clarity, or use 'Request'/'Response' suffix."
                                    .formatted(unit.simpleName()),
                    unit.qualifiedName()));
        }

        return violations;
    }

    /**
     * Checks if the qualified name indicates a DTO package.
     */
    private boolean isInDtoPackage(String qualifiedName) {
        String lower = qualifiedName.toLowerCase();
        return lower.contains(".dto.")
                || lower.contains(".dtos.")
                || lower.contains(".transfer.")
                || lower.contains(".contract.")
                || lower.contains(".contracts.");
    }

    /**
     * Creates a rule violation with a synthetic source location.
     */
    private RuleViolation createViolation(String message, String qualifiedName) {
        SourceLocation location = SourceLocation.of(qualifiedName + ".java", 1, 1);
        return RuleViolation.of(RULE_ID, defaultSeverity(), message, location);
    }
}
