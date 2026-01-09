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

import io.hexaglue.spi.plugin.HexaGluePlugin;

/**
 * Service Provider Interface for enrichment plugins.
 *
 * <p>Enrichment plugins run between classification and code generation to add
 * semantic labels and properties to the classification results. This enables
 * more sophisticated code generation and architecture analysis.
 *
 * <p>Enrichment plugins can detect:
 * <ul>
 *   <li><b>Behavioral patterns</b>: Factory methods, validators, collection managers</li>
 *   <li><b>Quality attributes</b>: Immutability, side-effect freedom, complexity</li>
 *   <li><b>Architectural patterns</b>: Event publishing, ACL, aggregate boundaries</li>
 * </ul>
 *
 * <p>Typical implementation:
 * <pre>{@code
 * public class MyEnricher implements EnrichmentPlugin {
 *
 *     @Override
 *     public String id() {
 *         return "my.enricher";
 *     }
 *
 *     @Override
 *     public EnrichmentContribution enrich(EnrichmentContext context) {
 *         Map<String, Set<SemanticLabel>> labels = new HashMap<>();
 *
 *         // Analyze methods and add labels
 *         for (var method : context.classification().methods()) {
 *             if (isFactoryMethod(method)) {
 *                 labels.put(method.identifier(), Set.of(SemanticLabel.FACTORY_METHOD));
 *             }
 *         }
 *
 *         return new EnrichmentContribution(id(), labels, Map.of());
 *     }
 * }
 * }</pre>
 *
 * <p>Registration: Create {@code META-INF/services/io.hexaglue.spi.enrichment.EnrichmentPlugin}
 * containing the fully-qualified class name of your implementation.
 *
 * @since 3.0.0
 */
public interface EnrichmentPlugin extends HexaGluePlugin {

    /**
     * Performs enrichment analysis and returns contributions.
     *
     * <p>This method is called during the enrichment phase, after classification
     * but before code generation. Implementations should:
     * <ol>
     *   <li>Analyze the classification results</li>
     *   <li>Detect semantic patterns and properties</li>
     *   <li>Return an EnrichmentContribution with labels and properties</li>
     * </ol>
     *
     * <p>If the plugin encounters errors, it should report them via
     * {@code context.diagnostics()} and return an empty contribution rather
     * than throwing exceptions.
     *
     * @param context the enrichment context
     * @return the enrichment contribution (never null)
     */
    EnrichmentContribution enrich(EnrichmentContext context);
}
