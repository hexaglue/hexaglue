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
import java.util.List;

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
 *   <li>The types affected by the violation</li>
 *   <li>The source location where the violation occurred</li>
 * </ul>
 *
 * @param ruleId        the unique identifier of the violated rule
 * @param severity      the severity level (INFO, WARNING, ERROR)
 * @param message       the human-readable violation description
 * @param affectedTypes the fully qualified names of types affected by this violation
 * @param location      the source code location where the violation occurred
 * @since 3.0.0
 */
public record RuleViolation(
        String ruleId,
        Severity severity,
        String message,
        List<String> affectedTypes,
        SourceLocation location) {

    /**
     * Compact constructor with defensive copy.
     */
    public RuleViolation {
        affectedTypes = affectedTypes != null ? List.copyOf(affectedTypes) : List.of();
    }

    /**
     * Creates a RuleViolation without affected types (for backward compatibility).
     *
     * @param ruleId   the rule identifier
     * @param severity the severity level
     * @param message  the message
     * @param location the source location
     * @return a new RuleViolation with empty affected types
     * @since 5.0.0
     */
    public static RuleViolation of(String ruleId, Severity severity, String message, SourceLocation location) {
        return new RuleViolation(ruleId, severity, message, List.of(), location);
    }

    /**
     * Creates a violation with INFO severity.
     *
     * @param ruleId   the rule identifier
     * @param message  the message
     * @param location the source location
     * @return a new RuleViolation
     */
    public static RuleViolation info(String ruleId, String message, SourceLocation location) {
        return new RuleViolation(ruleId, Severity.INFO, message, List.of(), location);
    }

    /**
     * Creates a violation with INFO severity and affected types.
     *
     * @param ruleId        the rule identifier
     * @param message       the message
     * @param affectedTypes the affected type names
     * @param location      the source location
     * @return a new RuleViolation
     * @since 5.0.0
     */
    public static RuleViolation info(
            String ruleId, String message, List<String> affectedTypes, SourceLocation location) {
        return new RuleViolation(ruleId, Severity.INFO, message, affectedTypes, location);
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
        return new RuleViolation(ruleId, Severity.MAJOR, message, List.of(), location);
    }

    /**
     * Creates a violation with WARNING severity and affected types.
     *
     * @param ruleId        the rule identifier
     * @param message       the message
     * @param affectedTypes the affected type names
     * @param location      the source location
     * @return a new RuleViolation
     * @since 5.0.0
     */
    public static RuleViolation warning(
            String ruleId, String message, List<String> affectedTypes, SourceLocation location) {
        return new RuleViolation(ruleId, Severity.MAJOR, message, affectedTypes, location);
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
        return new RuleViolation(ruleId, Severity.CRITICAL, message, List.of(), location);
    }

    /**
     * Creates a violation with ERROR severity and affected types.
     *
     * @param ruleId        the rule identifier
     * @param message       the message
     * @param affectedTypes the affected type names
     * @param location      the source location
     * @return a new RuleViolation
     * @since 5.0.0
     */
    public static RuleViolation error(
            String ruleId, String message, List<String> affectedTypes, SourceLocation location) {
        return new RuleViolation(ruleId, Severity.CRITICAL, message, affectedTypes, location);
    }

    /**
     * Returns true if this violation is an error (BLOCKER or CRITICAL).
     *
     * @return true if severity is BLOCKER or CRITICAL
     */
    public boolean isError() {
        return severity == Severity.BLOCKER || severity == Severity.CRITICAL;
    }

    /**
     * Returns true if this violation is a warning (MAJOR).
     *
     * @return true if severity is MAJOR
     */
    public boolean isWarning() {
        return severity == Severity.MAJOR;
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
