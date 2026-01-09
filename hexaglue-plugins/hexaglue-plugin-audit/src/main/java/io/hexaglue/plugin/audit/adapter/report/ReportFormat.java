/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.report;

import java.util.Objects;

/**
 * Supported report formats for audit output.
 *
 * <p>Each format has an associated file extension and default filename.
 * The CONSOLE format is special - it has no file output.
 *
 * @since 1.0.0
 */
public enum ReportFormat {
    /**
     * JSON format for programmatic consumption.
     */
    JSON("json", "audit-report.json"),

    /**
     * Self-contained HTML report with embedded CSS.
     */
    HTML("html", "audit-report.html"),

    /**
     * GitHub-flavored Markdown format.
     */
    MARKDOWN("md", "AUDIT-REPORT.md"),

    /**
     * Console output with ANSI colors (no file).
     */
    CONSOLE("console", null);

    private final String extension;
    private final String defaultFilename;

    ReportFormat(String extension, String defaultFilename) {
        this.extension = Objects.requireNonNull(extension, "extension required");
        this.defaultFilename = defaultFilename;
    }

    /**
     * Returns the file extension for this format.
     *
     * @return the extension (e.g., "json", "html")
     */
    public String extension() {
        return extension;
    }

    /**
     * Returns the default filename for this format.
     *
     * @return the default filename, or null for CONSOLE format
     */
    public String defaultFilename() {
        return defaultFilename;
    }

    /**
     * Returns true if this format produces a file.
     *
     * @return true for all formats except CONSOLE
     */
    public boolean hasFileOutput() {
        return defaultFilename != null;
    }
}
