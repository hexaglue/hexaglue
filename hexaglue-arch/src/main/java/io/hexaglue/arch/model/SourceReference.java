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

package io.hexaglue.arch.model;

import java.util.Objects;

/**
 * A reference to a location in source code.
 *
 * <p>This record provides a simplified view of source location information,
 * suitable for use in generated documentation and reports. It captures the
 * essential file path and line range for a code element.</p>
 *
 * @param filePath the file path (may be absolute or relative)
 * @param lineStart the starting line number (1-based)
 * @param lineEnd the ending line number (1-based)
 * @since 5.0.0
 */
public record SourceReference(String filePath, int lineStart, int lineEnd) {

    /**
     * Creates a new SourceReference.
     *
     * @param filePath the file path, must not be null
     * @param lineStart the starting line number
     * @param lineEnd the ending line number
     * @throws NullPointerException if filePath is null
     */
    public SourceReference {
        Objects.requireNonNull(filePath, "filePath must not be null");
    }

    /**
     * Returns the file name without the directory path.
     *
     * @return the file name
     */
    public String fileName() {
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash < 0 ? filePath : filePath.substring(lastSlash + 1);
    }

    /**
     * Returns a human-readable display string (e.g., "Order.java:42").
     *
     * @return the display string
     */
    public String toDisplayString() {
        return fileName() + ":" + lineStart;
    }
}
