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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;
import java.util.Optional;

/**
 * Location of a source element related to an issue.
 *
 * @param type fully qualified type name
 * @param file path to the source file
 * @param line line number (may be null if unknown)
 * @since 5.0.0
 */
public record SourceLocation(String type, String file, Integer line) {

    /**
     * Creates a source location with validation.
     */
    public SourceLocation {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(file, "file is required");
    }

    /**
     * Creates a source location without line number.
     *
     * @param type type name
     * @param file file path
     * @return source location
     */
    public static SourceLocation of(String type, String file) {
        return new SourceLocation(type, file, null);
    }

    /**
     * Creates a source location with line number.
     *
     * @param type type name
     * @param file file path
     * @param line line number
     * @return source location
     */
    public static SourceLocation at(String type, String file, int line) {
        return new SourceLocation(type, file, line);
    }

    /**
     * Returns the line number as optional.
     *
     * @return optional line number
     */
    public Optional<Integer> lineOpt() {
        return Optional.ofNullable(line);
    }

    /**
     * Returns a formatted location string.
     *
     * @return formatted location
     */
    public String formatted() {
        if (line != null) {
            return type + " (line " + line + ")";
        }
        return type;
    }
}
