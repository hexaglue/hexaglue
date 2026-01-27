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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Verdict section of the audit report containing score and status.
 *
 * @param score overall health score (0-100)
 * @param grade letter grade (A, B, C, D, F)
 * @param status pass/fail status
 * @param statusReason explanation of why the status is what it is
 * @param summary human-readable summary of the audit results
 * @param kpis list of key performance indicators
 * @param immediateAction action required if status is FAILED
 * @since 5.0.0
 */
public record Verdict(
        int score,
        String grade,
        ReportStatus status,
        String statusReason,
        String summary,
        List<KPI> kpis,
        ImmediateAction immediateAction) {

    /**
     * Creates a verdict with validation.
     */
    public Verdict {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        Objects.requireNonNull(grade, "grade is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(summary, "summary is required");
        kpis = kpis != null ? List.copyOf(kpis) : List.of();
        if (immediateAction == null) {
            immediateAction = ImmediateAction.none();
        }
    }

    /**
     * Computes the grade from a score.
     *
     * @param score the score (0-100)
     * @return letter grade
     */
    public static String computeGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    /**
     * Returns the immediate action if required.
     *
     * @return optional immediate action
     */
    public Optional<ImmediateAction> immediateActionOpt() {
        if (immediateAction != null && immediateAction.required()) {
            return Optional.of(immediateAction);
        }
        return Optional.empty();
    }
}
