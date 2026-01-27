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

package io.hexaglue.plugin.audit.adapter.diagram;

import io.hexaglue.plugin.audit.domain.model.report.ViolationCounts;

/**
 * Builds Mermaid pie charts for violation distribution visualization.
 *
 * @since 5.0.0
 */
public class PieChartBuilder {

    /**
     * Builds a pie chart showing violation distribution by severity.
     *
     * @param counts violation counts by severity
     * @return Mermaid pie diagram code (without code fence)
     */
    public String build(ViolationCounts counts) {
        StringBuilder sb = new StringBuilder();
        sb.append("pie showData title Violations by Severity\n");

        // Only add non-zero segments
        if (counts.blockers() > 0) {
            sb.append("    \"BLOCKER\" : ").append(counts.blockers()).append("\n");
        }
        if (counts.criticals() > 0) {
            sb.append("    \"CRITICAL\" : ").append(counts.criticals()).append("\n");
        }
        if (counts.majors() > 0) {
            sb.append("    \"MAJOR\" : ").append(counts.majors()).append("\n");
        }
        if (counts.minors() > 0) {
            sb.append("    \"MINOR\" : ").append(counts.minors()).append("\n");
        }
        if (counts.infos() > 0) {
            sb.append("    \"INFO\" : ").append(counts.infos()).append("\n");
        }

        // Handle case where all counts are zero
        if (counts.total() == 0) {
            sb.append("    \"No violations\" : 1\n");
        }

        return sb.toString().trim();
    }

    /**
     * Builds a pie chart with custom title.
     *
     * @param counts violation counts by severity
     * @param title custom chart title
     * @return Mermaid pie diagram code (without code fence)
     */
    public String build(ViolationCounts counts, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("pie showData title ").append(title).append("\n");

        if (counts.blockers() > 0) {
            sb.append("    \"BLOCKER\" : ").append(counts.blockers()).append("\n");
        }
        if (counts.criticals() > 0) {
            sb.append("    \"CRITICAL\" : ").append(counts.criticals()).append("\n");
        }
        if (counts.majors() > 0) {
            sb.append("    \"MAJOR\" : ").append(counts.majors()).append("\n");
        }
        if (counts.minors() > 0) {
            sb.append("    \"MINOR\" : ").append(counts.minors()).append("\n");
        }
        if (counts.infos() > 0) {
            sb.append("    \"INFO\" : ").append(counts.infos()).append("\n");
        }

        if (counts.total() == 0) {
            sb.append("    \"No violations\" : 1\n");
        }

        return sb.toString().trim();
    }
}
