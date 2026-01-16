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

package io.hexaglue.core.classification.engine;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A contribution from a classification criteria.
 *
 * <p>This is a generic record that captures the result of evaluating a criteria
 * against a type. It includes all information needed by the {@link DecisionPolicy}
 * to make a classification decision.
 *
 * <p>Metadata can be used to attach additional information like port direction.
 *
 * @param <K> the kind type (e.g., ElementKind, PortKind)
 * @param kind the classification kind this contribution votes for
 * @param criteriaName the name of the criteria that produced this contribution
 * @param priority the priority of the criteria (higher wins)
 * @param confidence the confidence level of the match
 * @param justification human-readable explanation of why this criteria matched
 * @param evidence list of evidence supporting this contribution
 * @param metadata additional key-value pairs (e.g., "direction" for ports)
 */
public record Contribution<K>(
        K kind,
        String criteriaName,
        int priority,
        ConfidenceLevel confidence,
        String justification,
        List<Evidence> evidence,
        Map<String, Object> metadata) {

    public Contribution {
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(criteriaName, "criteriaName cannot be null");
        Objects.requireNonNull(confidence, "confidence cannot be null");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Creates a simple contribution without evidence or metadata.
     *
     * @param kind the classification kind
     * @param criteria the criteria name
     * @param priority the criteria priority
     * @param confidence the confidence level
     * @param justification the justification text
     * @param <K> the kind type
     * @return a new contribution
     */
    public static <K> Contribution<K> of(
            K kind, String criteria, int priority, ConfidenceLevel confidence, String justification) {
        return new Contribution<>(kind, criteria, priority, confidence, justification, List.of(), Map.of());
    }

    /**
     * Creates a contribution with evidence but no metadata.
     *
     * @param kind the classification kind
     * @param criteria the criteria name
     * @param priority the criteria priority
     * @param confidence the confidence level
     * @param justification the justification text
     * @param evidence the list of evidence
     * @param <K> the kind type
     * @return a new contribution
     */
    public static <K> Contribution<K> of(
            K kind,
            String criteria,
            int priority,
            ConfidenceLevel confidence,
            String justification,
            List<Evidence> evidence) {
        return new Contribution<>(kind, criteria, priority, confidence, justification, evidence, Map.of());
    }

    /**
     * Creates a new contribution with an additional metadata entry.
     *
     * <p>This method is immutable - it returns a new contribution instance
     * with the metadata added.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return a new contribution with the metadata added
     */
    public Contribution<K> withMetadata(String key, Object value) {
        Objects.requireNonNull(key, "metadata key cannot be null");
        Objects.requireNonNull(value, "metadata value cannot be null");
        var newMeta = new HashMap<>(metadata);
        newMeta.put(key, value);
        return new Contribution<>(
                kind, criteriaName, priority, confidence, justification, evidence, Map.copyOf(newMeta));
    }

    /**
     * Creates a new contribution with additional evidence.
     *
     * @param additionalEvidence the evidence to add
     * @return a new contribution with the evidence added
     */
    public Contribution<K> withEvidence(List<Evidence> additionalEvidence) {
        if (additionalEvidence == null || additionalEvidence.isEmpty()) {
            return this;
        }
        var newEvidence = new java.util.ArrayList<>(evidence);
        newEvidence.addAll(additionalEvidence);
        return new Contribution<>(kind, criteriaName, priority, confidence, justification, newEvidence, metadata);
    }

    /**
     * Retrieves a typed metadata value.
     *
     * @param key the metadata key
     * @param type the expected type
     * @param <T> the type parameter
     * @return the value if present and of the correct type, empty otherwise
     */
    public <T> Optional<T> metadata(String key, Class<T> type) {
        return Optional.ofNullable(metadata.get(key)).filter(type::isInstance).map(type::cast);
    }

    /**
     * Checks if this contribution has a specific metadata key.
     *
     * @param key the metadata key
     * @return true if the key exists
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
}
