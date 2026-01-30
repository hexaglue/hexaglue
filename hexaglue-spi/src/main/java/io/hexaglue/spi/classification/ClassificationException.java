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

/**
 * Exception thrown when classification fails critically.
 *
 * <p>This exception should be used sparingly by secondary classifiers.
 * In most cases, returning {@link SecondaryClassificationResult#unclassified()}
 * is preferred over throwing an exception.
 *
 * <p>Throw this exception only when:
 * <ul>
 *   <li>The classifier encounters invalid configuration</li>
 *   <li>Required resources are unavailable</li>
 *   <li>The classification context is corrupted</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>When a secondary classifier throws {@code ClassificationException}:
 * <ul>
 *   <li>The exception is caught and logged</li>
 *   <li>The primary classification result is used (if available)</li>
 *   <li>Otherwise, the type is marked as unclassified</li>
 *   <li>Classification continues for other types</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public Optional<SecondaryClassificationResult> classify(
 *         TypeInfo type,
 *         ClassificationContext context,
 *         Optional<PrimaryClassificationResult> primaryResult) {
 *
 *     if (context.classifiedCount() == 0) {
 *         throw new ClassificationException(
 *             "Classification context is empty - cannot perform graph analysis");
 *     }
 *
 *     // ... classification logic
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
public class ClassificationException extends RuntimeException {

    /**
     * Constructs a new classification exception with the specified message.
     *
     * @param message the detail message
     */
    public ClassificationException(String message) {
        super(message);
    }

    /**
     * Constructs a new classification exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause (which is saved for later retrieval)
     */
    public ClassificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
