/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.report.model;

import java.util.Objects;

/**
 * A single violation entry in the audit report.
 *
 * @param constraintId  the ID of the violated constraint
 * @param severity      the severity level (BLOCKER, CRITICAL, MAJOR, MINOR, INFO)
 * @param message       the violation message
 * @param affectedType  the primary affected type (qualified name)
 * @param location      the source location (file:line:column format)
 * @param evidence      textual evidence supporting the violation
 * @since 1.0.0
 */
public record ViolationEntry(
        String constraintId,
        String severity,
        String message,
        String affectedType,
        String location,
        String evidence) {

    public ViolationEntry {
        Objects.requireNonNull(constraintId, "constraintId required");
        Objects.requireNonNull(severity, "severity required");
        Objects.requireNonNull(message, "message required");
        Objects.requireNonNull(affectedType, "affectedType required");
        Objects.requireNonNull(location, "location required");
        Objects.requireNonNull(evidence, "evidence required");
    }
}
