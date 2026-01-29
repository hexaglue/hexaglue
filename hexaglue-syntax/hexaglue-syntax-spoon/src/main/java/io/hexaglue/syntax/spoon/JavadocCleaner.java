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

package io.hexaglue.syntax.spoon;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility for cleaning raw Javadoc content from Spoon's {@code getDocComment()}.
 *
 * <p>Strips leading asterisks, removes Javadoc tags ({@literal @param}, {@literal @return}, etc.),
 * and joins the description lines into a single string.</p>
 *
 * @since 5.0.0
 */
final class JavadocCleaner {

    private JavadocCleaner() {
        // utility class
    }

    /**
     * Cleans a raw Javadoc string, keeping only the description part.
     *
     * @param raw the raw Javadoc content (may be null or blank)
     * @return the cleaned documentation, or empty if no meaningful content
     */
    static Optional<String> clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String cleaned = Arrays.stream(raw.split("\n"))
                .map(String::trim)
                .map(line -> line.startsWith("*") ? line.substring(1).trim() : line)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("@"))
                .collect(Collectors.joining(" "));
        return cleaned.isEmpty() ? Optional.empty() : Optional.of(cleaned);
    }
}
