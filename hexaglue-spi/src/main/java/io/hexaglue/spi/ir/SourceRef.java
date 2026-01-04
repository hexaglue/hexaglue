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

package io.hexaglue.spi.ir;

/**
 * Reference to a source location for diagnostics.
 *
 * @param filePath the file path (relative or absolute)
 * @param lineStart the starting line number (1-based)
 * @param lineEnd the ending line number (1-based)
 */
public record SourceRef(String filePath, int lineStart, int lineEnd) {

    private static final SourceRef UNKNOWN = new SourceRef("<unknown>", 0, 0);

    /**
     * Returns a source reference for unknown/unavailable source location.
     * Use this for synthetic types or when source information is not available.
     */
    public static SourceRef unknown() {
        return UNKNOWN;
    }

    /**
     * Returns a source reference for synthetic/generated elements.
     *
     * @param description a short description of the synthetic source
     */
    public static SourceRef synthetic(String description) {
        return new SourceRef("<synthetic:" + description + ">", 0, 0);
    }

    /**
     * Returns a source reference for a single line.
     */
    public static SourceRef ofLine(String filePath, int line) {
        return new SourceRef(filePath, line, line);
    }

    /**
     * Returns true if this source reference represents an unknown or synthetic location.
     */
    public boolean isUnknown() {
        return this == UNKNOWN || filePath.startsWith("<");
    }

    /**
     * Returns true if this source reference represents a real source location.
     */
    public boolean isReal() {
        return !isUnknown();
    }
}
