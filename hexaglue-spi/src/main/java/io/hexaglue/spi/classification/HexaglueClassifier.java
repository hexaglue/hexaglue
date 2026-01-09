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

import io.hexaglue.spi.core.TypeInfo;
import java.time.Duration;
import java.util.Optional;

/**
 * User-provided secondary classifier for custom classification logic.
 *
 * <p>Secondary classifiers are invoked AFTER the primary (deterministic) classification
 * phase. They provide a mechanism for users to inject custom classification logic based
 * on domain-specific patterns, naming conventions, or other heuristics.
 *
 * <p>Secondary classifiers are particularly useful when:
 * <ul>
 *   <li>The primary classifier produces UNCERTAIN or NONE results</li>
 *   <li>Custom domain patterns need to be recognized</li>
 *   <li>Project-specific naming conventions should influence classification</li>
 *   <li>Multi-signal scoring based on various heuristics is needed</li>
 * </ul>
 *
 * <h2>Execution Model</h2>
 * <p>Secondary classifiers:
 * <ul>
 *   <li>Run with a configurable timeout (default: 1 second)</li>
 *   <li>Can choose to accept the primary result or override it</li>
 *   <li>Have access to all previously classified types for graph analysis</li>
 *   <li>Are isolated from each other and from the core engine</li>
 * </ul>
 *
 * <h2>Implementation Guidelines</h2>
 * <pre>{@code
 * public class MyClassifier implements HexaglueClassifier {
 *
 *     @Override
 *     public String id() {
 *         return "my-company.my-classifier";
 *     }
 *
 *     @Override
 *     public SecondaryClassificationResult classify(
 *             TypeInfo type,
 *             ClassificationContext context,
 *             Optional<PrimaryClassificationResult> primaryResult) {
 *
 *         // Trust high-certainty primary results
 *         if (primaryResult.isPresent() &&
 *             primaryResult.get().certainty().isReliable()) {
 *             return null; // Use primary result
 *         }
 *
 *         // Apply custom logic
 *         if (type.simpleName().endsWith("Aggregate")) {
 *             return new SecondaryClassificationResult(
 *                 DomainKind.AGGREGATE_ROOT,
 *                 CertaintyLevel.INFERRED,
 *                 ClassificationStrategy.WEIGHTED,
 *                 "Name ends with 'Aggregate'",
 *                 List.of(new ClassificationEvidence(
 *                     "naming_pattern", 5, "Suffix matches aggregate pattern"))
 *             );
 *         }
 *
 *         return SecondaryClassificationResult.unclassified();
 *     }
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
public interface HexaglueClassifier {

    /**
     * Classifies a type based on context and optional primary result.
     *
     * <p>This method is called for each type after primary classification completes.
     * It receives the primary classification result (if any) and can choose to:
     * <ul>
     *   <li>Return {@code null} to accept the primary result</li>
     *   <li>Return a new {@link SecondaryClassificationResult} to override</li>
     *   <li>Return {@link SecondaryClassificationResult#unclassified()} if unable to classify</li>
     * </ul>
     *
     * <p><b>Contract:</b>
     * <ul>
     *   <li>Must complete within the timeout specified by {@link #timeout()}</li>
     *   <li>Should not throw exceptions (use try-catch and return unclassified)</li>
     *   <li>Must be thread-safe if classifier is shared across invocations</li>
     *   <li>Should not perform expensive I/O operations</li>
     * </ul>
     *
     * @param type          the type to classify
     * @param context       the classification context with previously classified types
     * @param primaryResult the primary classification result, if available
     * @return the secondary classification result, or null to use primary
     * @throws ClassificationException if classification fails critically
     */
    SecondaryClassificationResult classify(
            TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primaryResult);

    /**
     * Maximum execution time for this classifier.
     *
     * <p>If classification takes longer than this duration, it will be cancelled
     * and the primary result (or unclassified) will be used instead.
     *
     * <p>Default: 1 second
     *
     * @return the timeout duration
     */
    default Duration timeout() {
        return Duration.ofSeconds(1);
    }

    /**
     * Classifier ID for logging and diagnostics.
     *
     * <p>Should be unique and descriptive. Recommended format:
     * {@code "company-name.classifier-name"} (e.g., {@code "acme.aggregate-detector"})
     *
     * @return unique classifier identifier
     */
    String id();
}
