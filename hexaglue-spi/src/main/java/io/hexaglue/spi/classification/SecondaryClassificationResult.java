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

package io.hexaglue.spi.classification;

import io.hexaglue.arch.ElementKind;
import java.util.List;
import java.util.Objects;

/**
 * Result of secondary (user-provided) classification.
 *
 * <p>Secondary classification results can:
 * <ul>
 *   <li>Override the primary classification with a new decision</li>
 *   <li>Signal to use the primary result (by returning {@code null})</li>
 *   <li>Indicate the type could not be classified (via {@link #unclassified()})</li>
 * </ul>
 *
 * <h2>Return Value Semantics</h2>
 * <p>When implementing {@link HexaglueClassifier#classify}, the return value has special meaning:
 * <ul>
 *   <li><b>{@code null}</b> - Accept the primary result (signal to use primary)</li>
 *   <li><b>New result</b> - Override the primary result with this classification</li>
 *   <li><b>{@link #unclassified()}</b> - Explicitly mark as unclassified</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Accept primary result
 * return null;
 *
 * // Or explicitly
 * return SecondaryClassificationResult.usePrimary();
 *
 * // Override with new classification
 * return new SecondaryClassificationResult(
 *     ElementKind.AGGREGATE_ROOT,
 *     CertaintyLevel.INFERRED,
 *     ClassificationStrategy.WEIGHTED,
 *     "Name matches aggregate pattern",
 *     List.of(new ClassificationEvidence("naming", 5, "Ends with Aggregate"))
 * );
 *
 * // Unable to classify
 * return SecondaryClassificationResult.unclassified();
 * }</pre>
 *
 * @param kind      the classified domain kind (null for unclassified)
 * @param certainty the certainty level of the classification
 * @param strategy  the strategy used to produce this classification
 * @param reasoning human-readable explanation of the classification
 * @param evidences list of evidence supporting this classification
 * @since 3.0.0
 */
public record SecondaryClassificationResult(
        ElementKind kind,
        CertaintyLevel certainty,
        ClassificationStrategy strategy,
        String reasoning,
        List<ClassificationEvidence> evidences) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any non-nullable parameter is null
     */
    public SecondaryClassificationResult {
        // kind can be null for unclassified types
        Objects.requireNonNull(certainty, "certainty required");
        Objects.requireNonNull(strategy, "strategy required");
        Objects.requireNonNull(reasoning, "reasoning required");
        Objects.requireNonNull(evidences, "evidences required");
    }

    /**
     * Creates an unclassified result.
     *
     * <p>Use this when the secondary classifier cannot determine a classification.
     * This is different from returning {@code null}, which signals to use the
     * primary result.
     *
     * @return unclassified result
     */
    public static SecondaryClassificationResult unclassified() {
        return new SecondaryClassificationResult(
                null,
                CertaintyLevel.NONE,
                ClassificationStrategy.UNCLASSIFIED,
                "No secondary classification",
                List.of());
    }

    /**
     * Signals to use the primary classification result.
     *
     * <p>This is equivalent to returning {@code null} from
     * {@link HexaglueClassifier#classify}, but makes the intent more explicit.
     *
     * <p><b>Note:</b> This method returns {@code null} by design. Callers should
     * check for null and fall back to the primary result.
     *
     * @return null (signals to use primary)
     */
    public static SecondaryClassificationResult usePrimary() {
        return null;
    }

    /**
     * Returns true if this result indicates successful classification.
     *
     * @return true if kind is not null and certainty is not NONE
     */
    public boolean isClassified() {
        return kind != null && certainty != CertaintyLevel.NONE;
    }

    /**
     * Returns true if this classification is reliable enough for code generation.
     *
     * @return true if certainty is EXPLICIT or CERTAIN_BY_STRUCTURE
     */
    public boolean isReliable() {
        return certainty.isReliable();
    }

    /**
     * Returns true if this classification should be reviewed by a developer.
     *
     * @return true if certainty is UNCERTAIN or NONE
     */
    public boolean needsReview() {
        return certainty.needsReview();
    }

    /**
     * Returns the total weight of all evidence.
     *
     * @return sum of all evidence weights
     */
    public int totalEvidenceWeight() {
        return evidences.stream().mapToInt(ClassificationEvidence::weight).sum();
    }
}
