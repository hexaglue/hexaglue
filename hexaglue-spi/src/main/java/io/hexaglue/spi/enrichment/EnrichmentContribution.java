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

package io.hexaglue.spi.enrichment;

import java.util.Map;
import java.util.Set;

/**
 * Enrichment contribution from a plugin.
 *
 * <p>Contributions include semantic labels and custom properties that augment
 * the classification results. Multiple plugins can contribute to the same type
 * or method, and their contributions are merged.
 *
 * <p>Example usage:
 * <pre>{@code
 * var contribution = new EnrichmentContribution(
 *     "my.plugin.id",
 *     Map.of(
 *         "Order.createOrder", Set.of(SemanticLabel.FACTORY_METHOD),
 *         "Order.validate", Set.of(SemanticLabel.INVARIANT_VALIDATOR)
 *     ),
 *     Map.of(
 *         "Order", Map.of("complexity-score", 42)
 *     )
 * );
 * }</pre>
 *
 * @param pluginId the ID of the contributing plugin
 * @param labels semantic labels keyed by type/method identifier
 * @param properties custom properties keyed by type/method identifier
 * @since 3.0.0
 */
public record EnrichmentContribution(
        String pluginId, Map<String, Set<SemanticLabel>> labels, Map<String, Map<String, Object>> properties) {

    /**
     * Creates an empty contribution.
     *
     * @param pluginId the plugin ID
     * @return an empty contribution
     */
    public static EnrichmentContribution empty(String pluginId) {
        return new EnrichmentContribution(pluginId, Map.of(), Map.of());
    }

    /**
     * Returns true if this contribution has no labels or properties.
     */
    public boolean isEmpty() {
        return labels.isEmpty() && properties.isEmpty();
    }

    /**
     * Returns the labels for the given identifier, or an empty set if none.
     */
    public Set<SemanticLabel> labelsFor(String identifier) {
        return labels.getOrDefault(identifier, Set.of());
    }

    /**
     * Returns the properties for the given identifier, or an empty map if none.
     */
    public Map<String, Object> propertiesFor(String identifier) {
        return properties.getOrDefault(identifier, Map.of());
    }
}
