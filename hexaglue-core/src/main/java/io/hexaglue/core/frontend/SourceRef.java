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

package io.hexaglue.core.frontend;

/**
 * Reference to a source code location.
 *
 * @param filePath the file path (may be relative or absolute)
 * @param lineStart the starting line number (1-based)
 * @param lineEnd the ending line number (1-based)
 */
public record SourceRef(String filePath, int lineStart, int lineEnd) {

    /**
     * Creates a source reference for a single line.
     */
    public static SourceRef ofLine(String filePath, int line) {
        return new SourceRef(filePath, line, line);
    }

    /**
     * Returns the file name without path.
     */
    public String fileName() {
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash < 0 ? filePath : filePath.substring(lastSlash + 1);
    }

}
