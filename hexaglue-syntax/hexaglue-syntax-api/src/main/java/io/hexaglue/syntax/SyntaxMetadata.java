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
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Metadata about a syntax analysis.
 *
 * @param basePackage the base package analyzed
 * @param sourcePaths the source paths analyzed
 * @param typeCount the number of types analyzed
 * @param analysisTime the time when analysis completed
 * @param parserName the name of the parser used
 * @since 4.0.0
 */
public record SyntaxMetadata(
        String basePackage, List<Path> sourcePaths, int typeCount, Instant analysisTime, String parserName) {

    public SyntaxMetadata {
        Objects.requireNonNull(basePackage, "basePackage");
        sourcePaths = sourcePaths != null ? List.copyOf(sourcePaths) : List.of();
        Objects.requireNonNull(parserName, "parserName");
    }
}
