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

import java.util.Objects;

/**
 * Factory for creating report generators.
 *
 * <p>This factory uses a simple switch expression to instantiate the appropriate
 * generator for each format.
 *
 * @since 1.0.0
 * @deprecated Use the new rendering pipeline with {@link ReportRenderer} implementations.
 *             This factory will be removed in 6.0.0.
 */
@Deprecated(since = "5.0.0", forRemoval = true)
public final class ReportGeneratorFactory {

    private ReportGeneratorFactory() {
        // Utility class - no instantiation
    }

    /**
     * Creates a report generator for the specified format.
     *
     * @param format the report format
     * @return a new report generator instance
     * @throws NullPointerException if format is null
     */
    public static ReportGenerator create(ReportFormat format) {
        Objects.requireNonNull(format, "format required");

        return switch (format) {
            case JSON -> new JsonReportGenerator();
            case HTML -> new HtmlReportGenerator();
            case MARKDOWN -> new MarkdownReportGenerator();
            case CONSOLE -> new ConsoleReportGenerator();
        };
    }
}
