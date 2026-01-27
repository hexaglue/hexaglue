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

package io.hexaglue.plugin.audit.adapter.report;

import io.hexaglue.plugin.audit.domain.model.report.DiagramSet;
import io.hexaglue.plugin.audit.domain.model.report.ReportData;

/**
 * Interface for rendering audit reports to different formats.
 *
 * <p>Implementations convert structured {@link ReportData} to various output
 * formats such as JSON, HTML, Markdown, or console output.
 *
 * @since 5.0.0
 */
public interface ReportRenderer {

    /**
     * Returns the output format this renderer produces.
     *
     * @return the report format
     */
    ReportFormat format();

    /**
     * Renders the report data to a string.
     *
     * <p>This method renders the report without diagrams. Use
     * {@link #render(ReportData, DiagramSet)} to include Mermaid diagrams.
     *
     * @param data the structured report data
     * @return the rendered report as a string
     */
    String render(ReportData data);

    /**
     * Renders the report data with diagrams to a string.
     *
     * <p>This method includes Mermaid diagrams in the output for formats
     * that support them (HTML, Markdown). For JSON format, diagrams are
     * typically not included in the output.
     *
     * @param data the structured report data
     * @param diagrams the pre-generated Mermaid diagrams
     * @return the rendered report as a string
     */
    String render(ReportData data, DiagramSet diagrams);
}
