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

import io.hexaglue.plugin.audit.domain.model.Severity;
import java.util.List;

/**
 * Summary counts of violations by severity.
 *
 * @param total total number of violations
 * @param blockers number of BLOCKER violations
 * @param criticals number of CRITICAL violations
 * @param majors number of MAJOR violations
 * @param minors number of MINOR violations
 * @param infos number of INFO violations
 * @since 5.0.0
 */
public record ViolationCounts(int total, int blockers, int criticals, int majors, int minors, int infos) {

    /**
     * Creates violation counts from a list of issue entries.
     *
     * @param issues list of issues
     * @return the counts
     */
    public static ViolationCounts fromIssues(List<IssueEntry> issues) {
        int blockers = 0, criticals = 0, majors = 0, minors = 0, infos = 0;
        for (IssueEntry issue : issues) {
            switch (issue.severity()) {
                case BLOCKER -> blockers++;
                case CRITICAL -> criticals++;
                case MAJOR -> majors++;
                case MINOR -> minors++;
                case INFO -> infos++;
            }
        }
        return new ViolationCounts(issues.size(), blockers, criticals, majors, minors, infos);
    }

    /**
     * Creates empty violation counts.
     *
     * @return empty counts
     */
    public static ViolationCounts empty() {
        return new ViolationCounts(0, 0, 0, 0, 0, 0);
    }

    /**
     * Checks if there are any blocking issues.
     *
     * @return true if blockers > 0
     */
    public boolean hasBlockers() {
        return blockers > 0;
    }

    /**
     * Checks if there are any critical or blocking issues.
     *
     * @return true if blockers > 0 or criticals > 0
     */
    public boolean hasCriticalOrBlocker() {
        return blockers > 0 || criticals > 0;
    }

    /**
     * Returns the count for a specific severity.
     *
     * @param severity the severity
     * @return count for that severity
     */
    public int countFor(Severity severity) {
        return switch (severity) {
            case BLOCKER -> blockers;
            case CRITICAL -> criticals;
            case MAJOR -> majors;
            case MINOR -> minors;
            case INFO -> infos;
        };
    }
}
