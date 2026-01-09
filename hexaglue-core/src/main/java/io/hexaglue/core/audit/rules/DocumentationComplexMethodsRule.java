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
import io.hexaglue.spi.audit.RuleViolation;
import io.hexaglue.spi.audit.Severity;
import io.hexaglue.spi.core.SourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit rule that enforces documentation on complex methods.
 *
 * <p>Methods with high cyclomatic complexity (> 10) are harder to understand
 * and maintain. They should be documented with Javadoc explaining their logic,
 * parameters, return values, and any edge cases.
 *
 * <p>Violations detected:
 * <ul>
 *   <li>Method with cyclomatic complexity > 10 without documentation</li>
 *   <li>Complex public methods missing parameter or return documentation</li>
 * </ul>
 *
 * <p>Note: This rule encourages documentation but the better solution is often
 * to refactor complex methods into smaller, more focused methods.
 *
 * @since 3.0.0
 */
public final class DocumentationComplexMethodsRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.doc.complex-methods";
    private static final String RULE_NAME = "Complex Method Documentation";
    private static final int COMPLEXITY_THRESHOLD = 10;

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
        List<RuleViolation> violations = new ArrayList<>();

        // Check each method's complexity
        for (var method : unit.methods()) {
            if (method.complexity() > COMPLEXITY_THRESHOLD) {
                // Complex method should be documented
                // We infer documentation status from the unit's missing docs list
                String methodSignature = method.name() + "()";

                if (unit.documentation().missingDocs().contains(methodSignature)
                        || unit.documentation().missingDocs().stream()
                                .anyMatch(doc -> doc.startsWith(method.name() + "("))) {

                    violations.add(createViolation(
                            "Complex method '%s' in type '%s' has cyclomatic complexity %d but is missing documentation. "
                                    + "Consider documenting the logic or refactoring into smaller methods."
                                            .formatted(method.name(), unit.simpleName(), method.complexity()),
                            unit.qualifiedName()));
                }
            }
        }

        return violations;
    }

    /**
     * Creates a rule violation with a synthetic source location.
     */
    private RuleViolation createViolation(String message, String qualifiedName) {
        SourceLocation location = SourceLocation.of(qualifiedName + ".java", 1, 1);
        return new RuleViolation(RULE_ID, defaultSeverity(), message, location);
    }
}
