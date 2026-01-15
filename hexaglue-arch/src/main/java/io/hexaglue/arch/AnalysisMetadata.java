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

package io.hexaglue.arch;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Metadata about the architectural analysis process.
 *
 * <p>Captures information about when and how the analysis was performed,
 * useful for debugging, logging, and incremental builds.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * AnalysisMetadata metadata = new AnalysisMetadata(
 *     Instant.now(),
 *     Duration.ofMillis(1234),
 *     "Spoon",
 *     150,
 *     "4.0.0"
 * );
 * }</pre>
 *
 * @param analysisTime when the analysis was performed
 * @param duration how long the analysis took
 * @param parserName the name of the syntax parser used
 * @param typesAnalyzed the number of types analyzed
 * @param hexaglueVersion the HexaGlue version that performed the analysis
 * @since 4.0.0
 */
public record AnalysisMetadata(
        Instant analysisTime, Duration duration, String parserName, int typesAnalyzed, String hexaglueVersion) {

    /**
     * Creates a new AnalysisMetadata instance.
     *
     * @param analysisTime when analysis was performed, must not be null
     * @param duration the analysis duration, must not be null
     * @param parserName the parser used, must not be null or blank
     * @param typesAnalyzed the count of types, must be non-negative
     * @param hexaglueVersion the version string, must not be null
     * @throws NullPointerException if required fields are null
     * @throws IllegalArgumentException if parserName is blank or typesAnalyzed is negative
     */
    public AnalysisMetadata {
        Objects.requireNonNull(analysisTime, "analysisTime must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(parserName, "parserName must not be null");
        Objects.requireNonNull(hexaglueVersion, "hexaglueVersion must not be null");
        if (parserName.isBlank()) {
            throw new IllegalArgumentException("parserName must not be blank");
        }
        if (typesAnalyzed < 0) {
            throw new IllegalArgumentException("typesAnalyzed must be non-negative");
        }
    }

    /**
     * Creates metadata for the current analysis.
     *
     * @param startTime when analysis started
     * @param parserName the parser used
     * @param typesAnalyzed the count of types analyzed
     * @return a new AnalysisMetadata
     */
    public static AnalysisMetadata now(Instant startTime, String parserName, int typesAnalyzed) {
        return new AnalysisMetadata(
                startTime, Duration.between(startTime, Instant.now()), parserName, typesAnalyzed, "4.0.0-SNAPSHOT");
    }

    /**
     * Creates metadata for testing.
     *
     * @param typesAnalyzed the count of types
     * @return a new AnalysisMetadata
     */
    public static AnalysisMetadata forTesting(int typesAnalyzed) {
        return new AnalysisMetadata(Instant.now(), Duration.ofMillis(100), "Test", typesAnalyzed, "4.0.0-SNAPSHOT");
    }

    /**
     * Returns the duration in milliseconds.
     *
     * @return duration in milliseconds
     */
    public long durationMillis() {
        return duration.toMillis();
    }
}
