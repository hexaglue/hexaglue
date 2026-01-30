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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Context provided to secondary classifiers during classification.
 *
 * <p>This record provides access to the state of the classification process,
 * including all previously classified types. This allows secondary classifiers
 * to perform graph-based analysis and make decisions based on relationships
 * between types.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SecondaryClassificationResult classify(
 *         TypeInfo type,
 *         ClassificationContext context,
 *         Optional<PrimaryClassificationResult> primaryResult) {
 *
 *     // Check if type is referenced by an aggregate root
 *     boolean referencedByAggregate = context.alreadyClassified()
 *         .values()
 *         .stream()
 *         .anyMatch(r -> r.kind().filter(k -> k == ElementKind.AGGREGATE_ROOT).isPresent());
 *
 *     // Access all type names in the analyzed codebase
 *     Set<String> allTypes = context.allTypeNames();
 *
 *     // ... classification logic
 * }
 * }</pre>
 *
 * @param alreadyClassified map of type qualified names to their primary classification results
 * @param allTypeNames      set of all type qualified names in the analyzed codebase
 * @since 3.0.0
 */
public record ClassificationContext(
        Map<String, PrimaryClassificationResult> alreadyClassified, Set<String> allTypeNames) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any parameter is null
     */
    public ClassificationContext {
        Objects.requireNonNull(alreadyClassified, "alreadyClassified required");
        Objects.requireNonNull(allTypeNames, "allTypeNames required");
    }

    /**
     * Retrieves the primary classification result for a given type.
     *
     * @param qualifiedTypeName the fully qualified type name
     * @return the classification result if present
     */
    public Optional<PrimaryClassificationResult> getClassification(String qualifiedTypeName) {
        return Optional.ofNullable(alreadyClassified.get(qualifiedTypeName));
    }

    /**
     * Checks if a type has been classified.
     *
     * @param qualifiedTypeName the fully qualified type name
     * @return true if the type has a classification result
     */
    public boolean isClassified(String qualifiedTypeName) {
        return alreadyClassified.containsKey(qualifiedTypeName);
    }

    /**
     * Returns the number of classified types.
     *
     * @return count of classified types
     */
    public int classifiedCount() {
        return alreadyClassified.size();
    }

    /**
     * Returns the total number of types in the analyzed codebase.
     *
     * @return count of all types
     */
    public int totalTypeCount() {
        return allTypeNames.size();
    }

    /**
     * Creates an empty context for testing.
     *
     * @return empty classification context
     */
    public static ClassificationContext empty() {
        return new ClassificationContext(Map.of(), Set.of());
    }
}
