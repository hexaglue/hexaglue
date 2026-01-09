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
 * Audit rule that enforces maximum method length (lines of code).
 *
 * <p>Long methods are harder to understand, test, and maintain. This rule
 * detects methods that exceed a reasonable length threshold and suggests
 * refactoring into smaller, more focused methods.
 *
 * <p>Guidelines:
 * <ul>
 *   <li>Clean Code (Robert C. Martin): Methods should be small, ideally 5-15 lines</li>
 *   <li>Pragmatic limit: 30 lines (configurable)</li>
 *   <li>Methods > 50 lines almost always need refactoring</li>
 * </ul>
 *
 * <p>Default threshold: 30 lines (configurable)
 *
 * <p><b>Note:</b> This rule uses the total LOC from CodeMetrics as a proxy for
 * method length. In a full implementation, individual method line counts would
 * be tracked separately.
 *
 * @since 3.0.0
 */
public final class ComplexityMethodLengthMaxRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.complexity.method-length-max";
    private static final String RULE_NAME = "Maximum Method Length";
    private static final int DEFAULT_THRESHOLD = 30;

    private final int threshold;

    /**
     * Creates a rule with the default threshold (30 lines).
     */
    public ComplexityMethodLengthMaxRule() {
        this(DEFAULT_THRESHOLD);
    }

    /**
     * Creates a rule with a custom threshold.
     *
     * @param threshold the maximum allowed lines of code per method
     */
    public ComplexityMethodLengthMaxRule(int threshold) {
        if (threshold < 1) {
            throw new IllegalArgumentException("Threshold must be >= 1, got: " + threshold);
        }
        this.threshold = threshold;
    }

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

        // Heuristic: If average method length exceeds threshold, report it
        // This is a simplified check since we don't have per-method LOC
        int methodCount = unit.metrics().numberOfMethods();
        if (methodCount > 0) {
            int totalLoc = unit.metrics().linesOfCode();
            int avgMethodLength = totalLoc / methodCount;

            // If average method length is high, check individual methods by complexity as proxy
            if (avgMethodLength > threshold) {
                for (var method : unit.methods()) {
                    // Use complexity as a proxy: complex methods are often long
                    if (method.complexity() > 10) {
                        violations.add(new RuleViolation(
                                RULE_ID,
                                defaultSeverity(),
                                "Method '%s' in type '%s' appears to be long (type has average %d lines/method, threshold is %d). "
                                        + "Consider refactoring into smaller methods."
                                                .formatted(
                                                        method.name(), unit.simpleName(), avgMethodLength, threshold),
                                SourceLocation.of(unit.qualifiedName() + ".java", 1, 1)));
                    }
                }
            }
        }

        return violations;
    }

    /**
     * Returns the configured threshold.
     *
     * @return the threshold value
     */
    public int threshold() {
        return threshold;
    }
}
