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

package io.hexaglue.core.classification.anomaly;

import io.hexaglue.spi.audit.Severity;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an architectural anomaly detected in the domain model.
 *
 * <p>An anomaly is a violation of DDD best practices or a potential design issue
 * that should be reviewed and potentially fixed. Anomalies include severity levels
 * to help prioritize remediation efforts.
 *
 * @param type          the type of anomaly
 * @param affectedType  the primary type affected by this anomaly
 * @param message       human-readable description of the issue
 * @param severity      the severity level
 * @param relatedTypes  other types involved in the anomaly
 * @since 3.0.0
 */
public record Anomaly(
        AnomalyType type, String affectedType, String message, Severity severity, List<String> relatedTypes) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any required parameter is null
     */
    public Anomaly {
        Objects.requireNonNull(type, "type required");
        Objects.requireNonNull(affectedType, "affectedType required");
        Objects.requireNonNull(message, "message required");
        Objects.requireNonNull(severity, "severity required");
        relatedTypes = relatedTypes != null ? Collections.unmodifiableList(List.copyOf(relatedTypes)) : List.of();
    }

    /**
     * Creates an ERROR-level anomaly.
     *
     * @param type         the anomaly type
     * @param affectedType the affected type
     * @param message      the message
     * @param relatedTypes related types
     * @return new anomaly with CRITICAL severity
     */
    public static Anomaly error(AnomalyType type, String affectedType, String message, List<String> relatedTypes) {
        return new Anomaly(type, affectedType, message, Severity.CRITICAL, relatedTypes);
    }

    /**
     * Creates a MAJOR-level anomaly.
     *
     * @param type         the anomaly type
     * @param affectedType the affected type
     * @param message      the message
     * @param relatedTypes related types
     * @return new anomaly with MAJOR severity
     */
    public static Anomaly warning(AnomalyType type, String affectedType, String message, List<String> relatedTypes) {
        return new Anomaly(type, affectedType, message, Severity.MAJOR, relatedTypes);
    }

    /**
     * Creates an INFO-level anomaly.
     *
     * @param type         the anomaly type
     * @param affectedType the affected type
     * @param message      the message
     * @param relatedTypes related types
     * @return new anomaly with INFO severity
     */
    public static Anomaly info(AnomalyType type, String affectedType, String message, List<String> relatedTypes) {
        return new Anomaly(type, affectedType, message, Severity.INFO, relatedTypes);
    }

    /**
     * Returns true if this anomaly is an error (BLOCKER or CRITICAL).
     *
     * @return true if severity is BLOCKER or CRITICAL
     */
    public boolean isError() {
        return severity == Severity.BLOCKER || severity == Severity.CRITICAL;
    }

    /**
     * Returns true if this anomaly is a warning (MAJOR).
     *
     * @return true if severity is MAJOR
     */
    public boolean isWarning() {
        return severity == Severity.MAJOR;
    }

    /**
     * Returns true if this anomaly should block code generation.
     *
     * <p>Only BLOCKER and CRITICAL-level anomalies block generation by default.
     *
     * @return true if severity is BLOCKER or CRITICAL
     */
    public boolean blocksGeneration() {
        return severity == Severity.BLOCKER || severity == Severity.CRITICAL;
    }

    /**
     * Returns a formatted string representation for reporting.
     *
     * @return formatted anomaly description
     */
    public String toReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] %s: %s\n", severity, type, affectedType));
        sb.append(String.format("  %s\n", message));
        if (!relatedTypes.isEmpty()) {
            sb.append("  Related types: ");
            sb.append(String.join(", ", relatedTypes));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%s[%s] %s: %s", severity, type, simplifyTypeName(affectedType), message);
    }

    private String simplifyTypeName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
