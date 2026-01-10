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

package io.hexaglue.core.enrichment;

import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.enrichment.SemanticLabel;
import java.util.*;

/**
 * Immutable snapshot of classification results enriched with semantic labels and properties.
 *
 * <p>This record wraps the primary classification results and adds:
 * <ul>
 *   <li>Semantic labels for types and methods (factory methods, validators, etc.)</li>
 *   <li>Custom properties contributed by enrichment plugins</li>
 *   <li>Convenience query methods for accessing enrichment data</li>
 * </ul>
 *
 * @param classifications primary classification results keyed by type name
 * @param labels semantic labels keyed by type/method identifier
 * @param properties custom properties keyed by type/method identifier
 */
public record EnrichedSnapshot(
        Map<String, PrimaryClassificationResult> classifications,
        Map<String, Set<SemanticLabel>> labels,
        Map<String, Map<String, Object>> properties) {

    /**
     * Creates an enriched snapshot with defensive copies.
     */
    public EnrichedSnapshot {
        classifications = Collections.unmodifiableMap(new HashMap<>(classifications));
        labels = Collections.unmodifiableMap(new HashMap<>(labels));
        properties = Collections.unmodifiableMap(new HashMap<>(properties));
    }

    /**
     * Creates an enriched snapshot from a list of classifications.
     *
     * @param classificationList list of classification results
     * @param labels semantic labels
     * @param properties custom properties
     * @return enriched snapshot with classifications indexed by type name
     */
    public static EnrichedSnapshot of(
            List<PrimaryClassificationResult> classificationList,
            Map<String, Set<SemanticLabel>> labels,
            Map<String, Map<String, Object>> properties) {
        Map<String, PrimaryClassificationResult> classificationsMap = new HashMap<>();
        for (PrimaryClassificationResult result : classificationList) {
            classificationsMap.put(result.typeName(), result);
        }
        return new EnrichedSnapshot(classificationsMap, labels, properties);
    }

    /**
     * Creates an empty enriched snapshot (for error cases).
     *
     * @param classificationList the classification results
     * @return an empty enriched snapshot
     */
    public static EnrichedSnapshot empty(List<PrimaryClassificationResult> classificationList) {
        return of(classificationList, Map.of(), Map.of());
    }

    /**
     * Returns the classification for the given type name.
     *
     * @param typeName the fully qualified type name
     * @return the classification result, or empty if not found
     */
    public Optional<PrimaryClassificationResult> classificationFor(String typeName) {
        return Optional.ofNullable(classifications.get(typeName));
    }

    /**
     * Returns all classification results as a list.
     *
     * @return list of all classification results
     */
    public List<PrimaryClassificationResult> allClassifications() {
        return List.copyOf(classifications.values());
    }

    /**
     * Returns the number of classifications.
     *
     * @return classification count
     */
    public int classificationCount() {
        return classifications.size();
    }

    /**
     * Checks if the given identifier has the specified label.
     *
     * @param identifier type or method identifier
     * @param label the semantic label to check
     * @return true if the identifier has the label
     */
    public boolean hasLabel(String identifier, SemanticLabel label) {
        return labels.getOrDefault(identifier, Set.of()).contains(label);
    }

    /**
     * Returns all labels for the given identifier.
     *
     * @param identifier type or method identifier
     * @return set of semantic labels (empty if none)
     */
    public Set<SemanticLabel> labelsFor(String identifier) {
        return labels.getOrDefault(identifier, Set.of());
    }

    /**
     * Returns a custom property value.
     *
     * @param identifier type or method identifier
     * @param key property key
     * @return the property value, or empty if not found
     */
    public Optional<Object> property(String identifier, String key) {
        return Optional.ofNullable(properties.getOrDefault(identifier, Map.of()).get(key));
    }

    /**
     * Returns a custom property value with type casting.
     *
     * @param identifier type or method identifier
     * @param key property key
     * @param type expected value type
     * @return the property value, or empty if not found or wrong type
     */
    public <T> Optional<T> property(String identifier, String key, Class<T> type) {
        return property(identifier, key).filter(type::isInstance).map(type::cast);
    }

    /**
     * Returns all properties for the given identifier.
     *
     * @param identifier type or method identifier
     * @return map of properties (empty if none)
     */
    public Map<String, Object> propertiesFor(String identifier) {
        return properties.getOrDefault(identifier, Map.of());
    }

    /**
     * Returns all identifiers that have the specified label.
     *
     * @param label the semantic label
     * @return set of identifiers with the label
     */
    public Set<String> identifiersWithLabel(SemanticLabel label) {
        Set<String> result = new HashSet<>();
        for (var entry : labels.entrySet()) {
            if (entry.getValue().contains(label)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Returns true if this snapshot has no labels or properties.
     */
    public boolean isEmpty() {
        return labels.isEmpty() && properties.isEmpty();
    }

    /**
     * Returns statistics about the enrichment.
     */
    public EnrichmentStats stats() {
        int totalLabels = labels.values().stream().mapToInt(Set::size).sum();
        int totalProperties =
                properties.values().stream().mapToInt(m -> m.size()).sum();

        Map<SemanticLabel, Integer> labelCounts = new HashMap<>();
        for (var labelSet : labels.values()) {
            for (var label : labelSet) {
                labelCounts.merge(label, 1, Integer::sum);
            }
        }

        return new EnrichmentStats(
                labels.size(),
                properties.size(),
                totalLabels,
                totalProperties,
                Collections.unmodifiableMap(labelCounts));
    }

    /**
     * Statistics about enrichment results.
     *
     * @param enrichedIdentifierCount number of identifiers with labels
     * @param identifiersWithPropertiesCount number of identifiers with properties
     * @param totalLabels total number of labels applied
     * @param totalProperties total number of properties set
     * @param labelCounts count of each semantic label
     */
    public record EnrichmentStats(
            int enrichedIdentifierCount,
            int identifiersWithPropertiesCount,
            int totalLabels,
            int totalProperties,
            Map<SemanticLabel, Integer> labelCounts) {}
}
