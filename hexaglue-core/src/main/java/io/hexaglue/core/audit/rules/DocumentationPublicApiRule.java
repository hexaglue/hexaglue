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
 * Audit rule that enforces documentation on public API elements.
 *
 * <p>Public APIs (interfaces, public classes, and their public methods) should
 * be documented with Javadoc to help users understand their purpose, contracts,
 * and usage. This rule detects missing documentation on public API elements.
 *
 * <p>Violations detected:
 * <ul>
 *   <li>Public interface without Javadoc</li>
 *   <li>Public class in API package without Javadoc</li>
 *   <li>Public methods without Javadoc (reported via documentation info)</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class DocumentationPublicApiRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.doc.public-api";
    private static final String RULE_NAME = "Public API Documentation";

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
        List<RuleViolation> violations = new ArrayList<>();

        // Check if this is a public API element
        boolean isPublicApi = isPublicApi(unit);

        if (!isPublicApi) {
            return List.of();
        }

        // Check if the type has documentation
        if (!unit.documentation().hasJavadoc()) {
            violations.add(createViolation(
                    "Public API type '%s' is missing Javadoc documentation. "
                            + "Public APIs should be well-documented to help users understand their purpose."
                                    .formatted(unit.simpleName()),
                    unit.qualifiedName()));
        }

        // Check for missing documentation on public elements
        if (unit.documentation().hasMissingDocs()) {
            for (String missingDoc : unit.documentation().missingDocs()) {
                violations.add(createViolation(
                        "Public API element '%s' in type '%s' is missing Javadoc documentation"
                                .formatted(missingDoc, unit.simpleName()),
                        unit.qualifiedName()));
            }
        }

        // Additional check: low documentation coverage
        if (unit.documentation().hasJavadoc() && unit.documentation().javadocCoverage() < 80) {
            violations.add(createViolation(
                    "Public API type '%s' has insufficient documentation coverage (%d%%). "
                            + "Aim for at least 80%% coverage on public APIs."
                                    .formatted(
                                            unit.simpleName(),
                                            unit.documentation().javadocCoverage()),
                    unit.qualifiedName()));
        }

        return violations;
    }

    /**
     * Determines if a code unit is part of the public API.
     */
    private boolean isPublicApi(CodeUnit unit) {
        // All interfaces are considered public API
        if (unit.kind() == CodeUnitKind.INTERFACE) {
            return true;
        }

        // Classes in api/spi packages are public API
        String qualifiedName = unit.qualifiedName();
        if (qualifiedName.contains(".api.") || qualifiedName.contains(".spi.")) {
            return true;
        }

        // Records in presentation layer are often DTOs and should be documented
        if (unit.kind() == CodeUnitKind.RECORD) {
            return true;
        }

        return false;
    }

    /**
     * Creates a rule violation with a synthetic source location.
     */
    private RuleViolation createViolation(String message, String qualifiedName) {
        SourceLocation location = SourceLocation.of(qualifiedName + ".java", 1, 1);
        return RuleViolation.of(RULE_ID, defaultSeverity(), message, location);
    }
}
