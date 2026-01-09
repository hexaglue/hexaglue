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

package io.hexaglue.core.classification.deterministic;

import io.hexaglue.core.classification.anomaly.Anomaly;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The complete result of deterministic classification.
 *
 * <p>This record contains:
 * <ul>
 *   <li>All classifications (map from type name to classification)</li>
 *   <li>Detected architectural anomalies</li>
 * </ul>
 *
 * <p>The result is immutable and can be queried for statistics and reporting.
 *
 * @param classifications map from qualified type name to classification
 * @param anomalies       list of detected anomalies
 * @since 3.0.0
 */
public record ClassificationResult(Map<String, Classification> classifications, List<Anomaly> anomalies) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any parameter is null
     */
    public ClassificationResult {
        Objects.requireNonNull(classifications, "classifications required");
        Objects.requireNonNull(anomalies, "anomalies required");
        classifications = Collections.unmodifiableMap(Map.copyOf(classifications));
        anomalies = Collections.unmodifiableList(List.copyOf(anomalies));
    }

    /**
     * Returns the classification for the given type, if present.
     *
     * @param qualifiedName the type's qualified name
     * @return the classification, or null if not classified
     */
    public Classification getClassification(String qualifiedName) {
        return classifications.get(qualifiedName);
    }

    /**
     * Returns true if the given type was classified.
     *
     * @param qualifiedName the type's qualified name
     * @return true if a classification exists
     */
    public boolean isClassified(String qualifiedName) {
        return classifications.containsKey(qualifiedName);
    }

    /**
     * Returns all error-level anomalies.
     *
     * @return list of errors
     */
    public List<Anomaly> getErrors() {
        return anomalies.stream().filter(Anomaly::isError).collect(Collectors.toList());
    }

    /**
     * Returns all warning-level anomalies.
     *
     * @return list of warnings
     */
    public List<Anomaly> getWarnings() {
        return anomalies.stream().filter(Anomaly::isWarning).collect(Collectors.toList());
    }

    /**
     * Returns true if there are any error-level anomalies.
     *
     * @return true if errors exist
     */
    public boolean hasErrors() {
        return anomalies.stream().anyMatch(Anomaly::isError);
    }

    /**
     * Returns true if there are any anomalies (errors or warnings).
     *
     * @return true if anomalies exist
     */
    public boolean hasAnomalies() {
        return !anomalies.isEmpty();
    }

    /**
     * Returns a summary of classification statistics.
     *
     * @return human-readable statistics
     */
    public String getStatistics() {
        long total = classifications.size();
        long classified = classifications.values().stream()
                .filter(Classification::isClassified)
                .count();
        long reliable = classifications.values().stream()
                .filter(Classification::isReliable)
                .count();
        long needsReview = classifications.values().stream()
                .filter(Classification::needsReview)
                .count();
        long errors = getErrors().size();
        long warnings = getWarnings().size();

        return String.format(
                "Classification Statistics:\n" + "  Total types: %d\n"
                        + "  Classified: %d (%.1f%%)\n"
                        + "  Reliable: %d (%.1f%%)\n"
                        + "  Needs review: %d (%.1f%%)\n"
                        + "  Anomalies: %d errors, %d warnings",
                total,
                classified,
                (classified * 100.0 / total),
                reliable,
                (reliable * 100.0 / total),
                needsReview,
                (needsReview * 100.0 / total),
                errors,
                warnings);
    }

    /**
     * Generates a detailed report of all classifications and anomalies.
     *
     * @return formatted report string
     */
    public String toReport() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== Classification Report ===\n\n");
        sb.append(getStatistics()).append("\n\n");

        if (!anomalies.isEmpty()) {
            sb.append("=== Anomalies ===\n\n");
            for (Anomaly anomaly : anomalies) {
                sb.append(anomaly.toReportString());
            }
            sb.append("\n");
        }

        sb.append("=== Classifications ===\n\n");
        classifications.values().stream()
                .sorted((a, b) -> {
                    // Sort by certainty (descending), then by type name
                    int certaintyCompare = Integer.compare(
                            b.certainty().ordinal(), a.certainty().ordinal());
                    if (certaintyCompare != 0) {
                        return certaintyCompare;
                    }
                    return a.typeName().compareTo(b.typeName());
                })
                .forEach(c -> {
                    sb.append(c.toSummaryString()).append("\n");
                    sb.append("  Reasoning: ").append(c.reasoning()).append("\n");
                    if (!c.evidences().isEmpty()) {
                        sb.append("  Evidence:\n");
                        c.evidences().forEach(e -> sb.append("    - ")
                                .append(e.description())
                                .append("\n"));
                    }
                    sb.append("\n");
                });

        return sb.toString();
    }
}
