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

package io.hexaglue.syntax;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Location of a source element in the codebase.
 *
 * @param filePath the path to the source file (may be null for generated types)
 * @param line the starting line number (1-based)
 * @param column the starting column number (1-based)
 * @param endLine the ending line number (1-based)
 * @param endColumn the ending column number (1-based)
 * @since 4.0.0
 */
public record SourceLocation(Path filePath, int line, int column, int endLine, int endColumn) {

    /**
     * Creates a source location with all fields.
     *
     * @param filePath the path to the source file
     * @param line the starting line number
     * @param column the starting column number
     * @param endLine the ending line number
     * @param endColumn the ending column number
     */
    public SourceLocation {
        // filePath can be null for generated/synthetic types
    }

    /**
     * Creates a source location with start position only.
     *
     * @param filePath the path to the source file
     * @param line the line number
     * @param column the column number
     * @return a new SourceLocation
     */
    public static SourceLocation at(Path filePath, int line, int column) {
        return new SourceLocation(filePath, line, column, line, column);
    }

    /**
     * Creates a source location for a line.
     *
     * @param filePath the path to the source file
     * @param line the line number
     * @return a new SourceLocation
     */
    public static SourceLocation atLine(Path filePath, int line) {
        return new SourceLocation(filePath, line, 1, line, 1);
    }

    /**
     * Creates an unknown source location.
     *
     * @return a SourceLocation with unknown position
     */
    public static SourceLocation unknown() {
        return new SourceLocation(null, 0, 0, 0, 0);
    }

    /**
     * Returns the file path, if known.
     *
     * @return an Optional containing the file path
     */
    public Optional<Path> path() {
        return Optional.ofNullable(filePath);
    }

    /**
     * Returns whether this location is known (has a file path).
     *
     * @return true if the file path is known
     */
    public boolean isKnown() {
        return filePath != null;
    }

    /**
     * Returns a string representation suitable for error messages.
     *
     * @return a formatted location string like "path/File.java:10:5"
     */
    public String toDisplayString() {
        if (filePath == null) {
            return "unknown";
        }
        return filePath.getFileName() + ":" + line + ":" + column;
    }
}
