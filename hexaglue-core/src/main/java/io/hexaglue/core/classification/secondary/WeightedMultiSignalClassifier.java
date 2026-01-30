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

package io.hexaglue.core.classification.secondary;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.classification.CertaintyLevel;
import io.hexaglue.arch.model.classification.ClassificationEvidence;
import io.hexaglue.arch.model.classification.ClassificationStrategy;
import io.hexaglue.arch.model.core.TypeInfo;
import io.hexaglue.arch.model.core.TypeKind;
import io.hexaglue.spi.classification.ClassificationContext;
import io.hexaglue.spi.classification.HexaglueClassifier;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.classification.SecondaryClassificationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Example secondary classifier using weighted multi-signal scoring.
 *
 * <p>This classifier demonstrates a practical approach to secondary classification
 * by combining multiple weak signals (naming patterns, type characteristics, etc.)
 * into a weighted score. It is particularly useful when primary classification
 * produces UNCERTAIN or NONE results.
 *
 * <h2>Classification Strategy</h2>
 * <p>The classifier evaluates three domain kinds:
 * <ul>
 *   <li><b>AGGREGATE_ROOT</b> - Based on naming patterns and reference patterns</li>
 *   <li><b>ENTITY</b> - Based on naming conventions</li>
 *   <li><b>VALUE_OBJECT</b> - Based on immutability signals (records, naming)</li>
 * </ul>
 *
 * <p>Each domain kind receives a score based on various signals. The highest
 * score wins, but only if it exceeds the minimum confidence threshold.
 *
 * <h2>When to Use</h2>
 * <p>This classifier is most effective for:
 * <ul>
 *   <li>Codebases with consistent naming conventions</li>
 *   <li>Types that lack explicit annotations or structural patterns</li>
 *   <li>Refining UNCERTAIN primary classifications</li>
 * </ul>
 *
 * <h2>Customization</h2>
 * <p>Extend this class and override scoring methods to adapt to your project's
 * specific patterns and conventions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // As a service loader entry
 * HexaglueClassifier classifier = new WeightedMultiSignalClassifier();
 *
 * // Or with custom threshold
 * HexaglueClassifier classifier = new WeightedMultiSignalClassifier(10);
 * }</pre>
 *
 * @since 3.0.0
 */
public class WeightedMultiSignalClassifier implements HexaglueClassifier {

    /**
     * Default minimum confidence threshold.
     * Scores below this threshold result in unclassified.
     */
    private static final int DEFAULT_MIN_CONFIDENCE_THRESHOLD = 7;

    private final int minConfidenceThreshold;

    /**
     * Creates a classifier with the default confidence threshold.
     */
    public WeightedMultiSignalClassifier() {
        this(DEFAULT_MIN_CONFIDENCE_THRESHOLD);
    }

    /**
     * Creates a classifier with a custom confidence threshold.
     *
     * @param minConfidenceThreshold minimum score required for classification
     */
    public WeightedMultiSignalClassifier(int minConfidenceThreshold) {
        this.minConfidenceThreshold = minConfidenceThreshold;
    }

    @Override
    public String id() {
        return "hexaglue.secondary.weighted-multi-signal";
    }

    @Override
    public Optional<SecondaryClassificationResult> classify(
            TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primaryResult) {

        // Trust high-certainty primary results
        if (primaryResult.isPresent()) {
            CertaintyLevel certainty = primaryResult.get().certainty();
            if (certainty == CertaintyLevel.CERTAIN_BY_STRUCTURE || certainty == CertaintyLevel.EXPLICIT) {
                return Optional.empty(); // Use primary
            }
        }

        // Collect evidence and score for each domain kind
        List<ClassificationEvidence> evidences = new ArrayList<>();

        int aggregateScore = scoreAsAggregateRoot(type, context, evidences);
        int entityScore = scoreAsEntity(type, context, evidences);
        int valueObjectScore = scoreAsValueObject(type, context, evidences);

        // Determine winner
        int maxScore = Math.max(aggregateScore, Math.max(entityScore, valueObjectScore));

        if (maxScore < minConfidenceThreshold) {
            return Optional.of(SecondaryClassificationResult.unclassified());
        }

        ElementKind kind;
        if (aggregateScore == maxScore) {
            kind = ElementKind.AGGREGATE_ROOT;
        } else if (entityScore == maxScore) {
            kind = ElementKind.ENTITY;
        } else {
            kind = ElementKind.VALUE_OBJECT;
        }

        return Optional.of(new SecondaryClassificationResult(
                kind,
                CertaintyLevel.INFERRED,
                ClassificationStrategy.WEIGHTED,
                String.format("Weighted scoring: %s=%d", kind, maxScore),
                evidences));
    }

    /**
     * Scores the type as a potential aggregate root.
     *
     * <p>Signals considered:
     * <ul>
     *   <li>Name contains "Aggregate" or ends with "Root" - strong signal (weight: 4)</li>
     *   <li>Name ends with common aggregate patterns - medium signal (weight: 3)</li>
     * </ul>
     *
     * @param type      the type to score
     * @param context   the classification context
     * @param evidences list to accumulate evidence
     * @return aggregate root score
     */
    protected int scoreAsAggregateRoot(
            TypeInfo type, ClassificationContext context, List<ClassificationEvidence> evidences) {

        int score = 0;
        String simpleName = type.simpleName();

        // Strong signal: explicit naming
        if (simpleName.contains("Aggregate") || simpleName.endsWith("Root")) {
            score += 4;
            evidences.add(new ClassificationEvidence(
                    "aggregate_naming_strong", 4, "Name contains 'Aggregate' or ends with 'Root'"));
        }

        // Medium signal: common aggregate patterns
        if (simpleName.endsWith("Order")
                || simpleName.endsWith("Account")
                || simpleName.endsWith("Invoice")
                || simpleName.endsWith("Booking")) {
            score += 3;
            evidences.add(
                    new ClassificationEvidence("aggregate_naming_pattern", 3, "Name matches common aggregate pattern"));
        }

        // Can add more signals here:
        // - Check if type is referenced by other types via ID
        // - Check for rich behavior (many methods)
        // - Check for lifecycle management methods

        return score;
    }

    /**
     * Scores the type as a potential entity.
     *
     * <p>Signals considered:
     * <ul>
     *   <li>Name ends with "Entity" - strong signal (weight: 4)</li>
     *   <li>Name ends with "Item" or "Line" - medium signal (weight: 2)</li>
     * </ul>
     *
     * @param type      the type to score
     * @param context   the classification context
     * @param evidences list to accumulate evidence
     * @return entity score
     */
    protected int scoreAsEntity(TypeInfo type, ClassificationContext context, List<ClassificationEvidence> evidences) {

        int score = 0;
        String simpleName = type.simpleName();

        // Strong signal: explicit naming
        if (simpleName.endsWith("Entity")) {
            score += 4;
            evidences.add(new ClassificationEvidence("entity_naming_strong", 4, "Name ends with 'Entity'"));
        }

        // Medium signal: common entity patterns (often child entities)
        if (simpleName.endsWith("Item") || simpleName.endsWith("Line")) {
            score += 2;
            evidences.add(new ClassificationEvidence("entity_naming_pattern", 2, "Name matches common entity pattern"));
        }

        return score;
    }

    /**
     * Scores the type as a potential value object.
     *
     * <p>Signals considered:
     * <ul>
     *   <li>Is a Java Record - very strong signal (weight: 5)</li>
     *   <li>Name ends with value object suffixes - strong signal (weight: 4)</li>
     *   <li>Name matches common value patterns - medium signal (weight: 3)</li>
     * </ul>
     *
     * @param type      the type to score
     * @param context   the classification context
     * @param evidences list to accumulate evidence
     * @return value object score
     */
    protected int scoreAsValueObject(
            TypeInfo type, ClassificationContext context, List<ClassificationEvidence> evidences) {

        int score = 0;
        String simpleName = type.simpleName();

        // Very strong signal: Java Record
        if (type.kind() == TypeKind.RECORD) {
            score += 5;
            evidences.add(new ClassificationEvidence("java_record", 5, "Java Record suggests immutable value"));
        }

        // Strong signal: explicit value object naming
        if (simpleName.endsWith("Value") || simpleName.endsWith("VO")) {
            score += 4;
            evidences.add(
                    new ClassificationEvidence("value_object_naming_strong", 4, "Name ends with 'Value' or 'VO'"));
        }

        // Medium signal: common value object patterns
        if (simpleName.endsWith("Dto")
                || simpleName.endsWith("Info")
                || simpleName.endsWith("Data")
                || simpleName.endsWith("Spec")) {
            score += 3;
            evidences.add(new ClassificationEvidence(
                    "value_object_naming_pattern", 3, "Name matches common value object pattern"));
        }

        // Medium signal: common domain value patterns
        if (simpleName.equals("Money")
                || simpleName.equals("Address")
                || simpleName.equals("Email")
                || simpleName.equals("Phone")
                || simpleName.endsWith("Amount")
                || simpleName.endsWith("Quantity")) {
            score += 3;
            evidences.add(new ClassificationEvidence(
                    "value_object_domain_pattern", 3, "Name matches common domain value pattern"));
        }

        return score;
    }

    /**
     * Returns the minimum confidence threshold.
     *
     * @return minimum score required for classification
     */
    public int getMinConfidenceThreshold() {
        return minConfidenceThreshold;
    }
}
