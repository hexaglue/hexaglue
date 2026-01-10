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

import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.generation.PluginCategory;
import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginContext;
import java.util.List;
import java.util.Optional;

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
     * Returns the plugin category.
     *
     * <p>Enrichment plugins always return {@link PluginCategory#ENRICHMENT}.
     *
     * @return {@link PluginCategory#ENRICHMENT}
     */
    @Override
    default PluginCategory category() {
        return PluginCategory.ENRICHMENT;
    }

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

    /**
     * Executes the plugin (delegates to enrich with context adaptation).
     *
     * <p>This method adapts the generic {@link PluginContext} to the specialized
     * {@link EnrichmentContext} required by enrichment plugins. It extracts
     * primary classification results from the plugin output store (populated by
     * the engine) and creates an EnrichmentContext for the plugin to analyze.
     *
     * <p>The implementation follows these steps:
     * <ol>
     *   <li>Retrieve primary classifications from the plugin output store</li>
     *   <li>Create an EnrichmentContext with classifications and architecture query</li>
     *   <li>Execute the enrichment analysis via {@link #enrich(EnrichmentContext)}</li>
     *   <li>Store the contribution in the output store for downstream plugins</li>
     * </ol>
     *
     * <p><b>Note:</b> Primary classifications must be stored in the output store
     * by the engine before enrichment plugins execute. The engine should call:
     * <pre>{@code
     * List<PrimaryClassificationResult> classifications =
     *     irExporter.exportPrimaryClassifications(classificationResults);
     * context.setOutput("primary-classifications", classifications);
     * }</pre>
     *
     * @param context the generic plugin context
     */
    @Override
    default void execute(PluginContext context) {
        try {
            // Retrieve primary classifications from the output store
            // The engine must populate this before enrichment plugins run
            Optional<?> rawOpt = context.getOutput("io.hexaglue.engine", "primary-classifications", List.class);

            if (rawOpt.isEmpty()) {
                context.diagnostics()
                        .error(
                                "Enrichment plugin execution failed: " + id(),
                                new IllegalStateException("Primary classifications not available in plugin context. "
                                        + "The engine must populate the output store with primary-classifications before "
                                        + "enrichment plugins execute."));
                return;
            }

            @SuppressWarnings("unchecked")
            List<PrimaryClassificationResult> classifications = (List<PrimaryClassificationResult>) rawOpt.get();

            // Create EnrichmentContext
            EnrichmentContext enrichmentContext = EnrichmentContext.of(
                    classifications, context.architectureQuery().orElse(null), context.diagnostics());

            // Execute the enrichment
            EnrichmentContribution contribution = enrich(enrichmentContext);

            // Store contribution for downstream plugins
            if (contribution != null) {
                context.setOutput("enrichment-contribution", contribution);
                context.diagnostics()
                        .info(String.format(
                                "Enrichment plugin %s completed: %d labels, %d properties",
                                id(),
                                contribution.labels().size(),
                                contribution.properties().size()));
            }

        } catch (Exception e) {
            context.diagnostics().error("Enrichment plugin execution failed: " + id(), e);
        }
    }
}
