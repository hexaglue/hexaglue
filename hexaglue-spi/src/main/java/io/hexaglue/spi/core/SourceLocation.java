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

package io.hexaglue.spi.core;

import java.util.Objects;

/**
 * Represents a location in source code.
 *
 * <p>This record provides precise location information for code elements,
 * enabling IDE integration, error reporting, and code navigation. It uses
 * 1-based line and column numbering consistent with most IDEs and editors.
 *
 * <p>Example usage in error messages:
 * <pre>{@code
 * SourceLocation loc = new SourceLocation("Order.java", 42, 42, 5, 20);
 * System.out.println(loc.toIdeFormat()); // "Order.java:42:5"
 * }</pre>
 *
 * @param filePath    the file path (relative or absolute)
 * @param lineStart   the starting line number (1-based)
 * @param lineEnd     the ending line number (1-based)
 * @param columnStart the starting column number (1-based)
 * @param columnEnd   the ending column number (1-based)
 * @since 3.0.0
 */
public record SourceLocation(String filePath, int lineStart, int lineEnd, int columnStart, int columnEnd) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException     if filePath is null
     * @throws IllegalArgumentException if line or column numbers are invalid
     */
    public SourceLocation {
        Objects.requireNonNull(filePath, "filePath required");
        if (lineStart < 1) {
            throw new IllegalArgumentException("lineStart must be >= 1, got: " + lineStart);
        }
        if (lineEnd < lineStart) {
            throw new IllegalArgumentException("lineEnd must be >= lineStart");
        }
        if (columnStart < 1) {
            throw new IllegalArgumentException("columnStart must be >= 1, got: " + columnStart);
        }
        if (columnEnd < columnStart) {
            throw new IllegalArgumentException("columnEnd must be >= columnStart");
        }
    }

    /**
     * Creates a source location for a single point (line and column).
     *
     * @param filePath the file path
     * @param line     the line number (1-based)
     * @param column   the column number (1-based)
     * @return a new SourceLocation
     */
    public static SourceLocation of(String filePath, int line, int column) {
        return new SourceLocation(filePath, line, line, column, column);
    }

    /**
     * Creates a source location for a line range.
     *
     * @param filePath  the file path
     * @param lineStart the starting line number (1-based)
     * @param lineEnd   the ending line number (1-based)
     * @return a new SourceLocation
     */
    public static SourceLocation ofLineRange(String filePath, int lineStart, int lineEnd) {
        return new SourceLocation(filePath, lineStart, lineEnd, 1, 1);
    }

    /**
     * Formats this location in IDE-compatible format: {@code file:line:column}.
     *
     * <p>This format is recognized by most IDEs and can be used in error messages
     * to enable click-to-navigate functionality.
     *
     * @return formatted location string (e.g., "Order.java:42:5")
     */
    public String toIdeFormat() {
        return "%s:%d:%d".formatted(filePath, lineStart, columnStart);
    }

    /**
     * Returns true if this location spans multiple lines.
     *
     * @return true if lineEnd > lineStart
     */
    public boolean isMultiLine() {
        return lineEnd > lineStart;
    }

    /**
     * Returns true if this location is a single point (one line, one column).
     *
     * @return true if both line and column ranges are single positions
     */
    public boolean isSinglePoint() {
        return lineStart == lineEnd && columnStart == columnEnd;
    }

    /**
     * Returns the number of lines spanned by this location.
     *
     * @return the line count (always >= 1)
     */
    public int lineCount() {
        return lineEnd - lineStart + 1;
    }
}
