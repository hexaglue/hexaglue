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
 * Audit rule that enforces maximum cyclomatic complexity threshold for methods.
 *
 * <p>Cyclomatic complexity measures the number of linearly independent paths
 * through a method's code. High complexity makes code harder to understand,
 * test, and maintain. This rule detects methods that exceed the complexity
 * threshold and suggests refactoring.
 *
 * <p>Thresholds (Thomas McCabe's recommendations):
 * <ul>
 *   <li>1-10: Simple, low risk</li>
 *   <li>11-20: Moderate complexity, medium risk</li>
 *   <li>21-50: Complex, high risk</li>
 *   <li>50+: Untestable, very high risk</li>
 * </ul>
 *
 * <p>Default threshold: 15 (configurable)
 *
 * @since 3.0.0
 */
public final class ComplexityCyclomaticMaxRule implements AuditRule {

    private static final String RULE_ID = "hexaglue.complexity.cyclomatic-max";
    private static final String RULE_NAME = "Maximum Cyclomatic Complexity";
    private static final int DEFAULT_THRESHOLD = 15;

    private final int threshold;

    /**
     * Creates a rule with the default threshold (15).
     */
    public ComplexityCyclomaticMaxRule() {
        this(DEFAULT_THRESHOLD);
    }

    /**
     * Creates a rule with a custom threshold.
     *
     * @param threshold the maximum allowed cyclomatic complexity
     */
    public ComplexityCyclomaticMaxRule(int threshold) {
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
        return Severity.WARNING;
    }

    @Override
    public List<RuleViolation> check(CodeUnit unit) {
        List<RuleViolation> violations = new ArrayList<>();

        for (var method : unit.methods()) {
            if (method.complexity() > threshold) {
                Severity severity = determineSeverity(method.complexity());

                violations.add(new RuleViolation(
                        RULE_ID,
                        severity,
                        "Method '%s' in type '%s' has cyclomatic complexity %d, which exceeds the threshold of %d. "
                                + "Consider refactoring into smaller methods."
                                        .formatted(method.name(), unit.simpleName(), method.complexity(), threshold),
                        SourceLocation.of(unit.qualifiedName() + ".java", 1, 1)));
            }
        }

        return violations;
    }

    /**
     * Determines severity based on complexity level.
     */
    private Severity determineSeverity(int complexity) {
        if (complexity > 50) {
            return Severity.ERROR; // Very high risk
        } else if (complexity > 20) {
            return Severity.WARNING; // High risk
        } else {
            return Severity.INFO; // Moderate risk
        }
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
