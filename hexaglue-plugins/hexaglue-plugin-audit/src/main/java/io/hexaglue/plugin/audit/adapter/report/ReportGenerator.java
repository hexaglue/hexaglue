/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.report;

import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Interface for generating audit reports in different formats.
 *
 * <p>Implementations convert an {@link AuditReport} into specific formats
 * like JSON, HTML, Markdown, or console output.
 *
 * @since 1.0.0
 */
public interface ReportGenerator {

    /**
     * Returns the format this generator produces.
     *
     * @return the report format
     */
    ReportFormat format();

    /**
     * Generates a report string from the audit data.
     *
     * @param report the audit report data
     * @return the formatted report as a string
     */
    String generate(AuditReport report);

    /**
     * Generates and writes the report to a file in the output directory.
     *
     * <p>The filename is determined by the format's default filename.
     * The output directory is created if it doesn't exist.
     *
     * @param report    the audit report data
     * @param outputDir the directory to write the report file to
     * @throws IOException              if file writing fails
     * @throws UnsupportedOperationException if the format has no file output (e.g., CONSOLE)
     */
    default void writeToFile(AuditReport report, Path outputDir) throws IOException {
        Objects.requireNonNull(report, "report required");
        Objects.requireNonNull(outputDir, "outputDir required");

        if (!format().hasFileOutput()) {
            throw new UnsupportedOperationException("Format " + format() + " does not support file output");
        }

        // Ensure output directory exists
        Files.createDirectories(outputDir);

        // Generate content
        String content = generate(report);

        // Write to file
        Path outputPath = outputDir.resolve(format().defaultFilename());
        Files.writeString(outputPath, content);
    }
}
