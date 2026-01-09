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

package io.hexaglue.spi.audit;

import io.hexaglue.spi.core.SourceLocation;

/**
 * Represents a violation of an audit rule.
 *
 * <p>Rule violations are detected during code auditing and represent issues
 * such as architecture violations, missing documentation, or quality concerns.
 * Each violation includes:
 * <ul>
 *   <li>The rule that was violated</li>
 *   <li>The severity of the violation</li>
 *   <li>A descriptive message</li>
 *   <li>The source location where the violation occurred</li>
 * </ul>
 *
 * @param ruleId   the unique identifier of the violated rule
 * @param severity the severity level (INFO, WARNING, ERROR)
 * @param message  the human-readable violation description
 * @param location the source code location where the violation occurred
 * @since 3.0.0
 */
public record RuleViolation(String ruleId, Severity severity, String message, SourceLocation location) {

    /**
     * Creates a violation with INFO severity.
     *
     * @param ruleId   the rule identifier
     * @param message  the message
     * @param location the source location
     * @return a new RuleViolation
     */
    public static RuleViolation info(String ruleId, String message, SourceLocation location) {
        return new RuleViolation(ruleId, Severity.INFO, message, location);
    }

    /**
     * Creates a violation with WARNING severity.
     *
     * @param ruleId   the rule identifier
     * @param message  the message
     * @param location the source location
     * @return a new RuleViolation
     */
    public static RuleViolation warning(String ruleId, String message, SourceLocation location) {
        return new RuleViolation(ruleId, Severity.WARNING, message, location);
    }

    /**
     * Creates a violation with ERROR severity.
     *
     * @param ruleId   the rule identifier
     * @param message  the message
     * @param location the source location
     * @return a new RuleViolation
     */
    public static RuleViolation error(String ruleId, String message, SourceLocation location) {
        return new RuleViolation(ruleId, Severity.ERROR, message, location);
    }

    /**
     * Returns true if this violation is an error.
     *
     * @return true if severity is ERROR
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

    /**
     * Returns true if this violation is a warning.
     *
     * @return true if severity is WARNING
     */
    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    /**
     * Returns true if this violation is informational.
     *
     * @return true if severity is INFO
     */
    public boolean isInfo() {
        return severity == Severity.INFO;
    }
}
