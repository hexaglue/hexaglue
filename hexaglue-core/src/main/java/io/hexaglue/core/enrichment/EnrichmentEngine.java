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

import io.hexaglue.core.audit.DefaultArchitectureQuery;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.enrichment.EnrichmentContext;
import io.hexaglue.spi.enrichment.EnrichmentContribution;
import io.hexaglue.spi.enrichment.EnrichmentPlugin;
import io.hexaglue.spi.enrichment.SemanticLabel;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import java.util.*;

/**
 * Coordinates enrichment plugins and merges their contributions.
 *
 * <p>The enrichment engine:
 * <ol>
 *   <li>Runs built-in behavioral pattern detection</li>
 *   <li>Discovers and executes enrichment plugins via ServiceLoader</li>
 *   <li>Merges contributions from all plugins</li>
 *   <li>Returns an immutable EnrichedSnapshot</li>
 * </ol>
 *
 * <p>Plugins can contribute:
 * <ul>
 *   <li><b>Labels</b>: Semantic labels applied to types and methods</li>
 *   <li><b>Properties</b>: Custom key-value properties</li>
 * </ul>
 */
public class EnrichmentEngine {

    private final ApplicationGraph graph;
    private final DiagnosticReporter diagnostics;
    private final List<EnrichmentPlugin> plugins;

    /**
     * Creates an enrichment engine.
     *
     * @param graph the application graph
     * @param diagnostics diagnostic reporter
     */
    public EnrichmentEngine(ApplicationGraph graph, DiagnosticReporter diagnostics) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics cannot be null");
        this.plugins = discoverPlugins();
    }

    /**
     * Creates an enrichment engine with explicit plugins (for testing).
     *
     * @param graph the application graph
     * @param diagnostics diagnostic reporter
     * @param plugins explicit list of plugins
     */
    public EnrichmentEngine(ApplicationGraph graph, DiagnosticReporter diagnostics, List<EnrichmentPlugin> plugins) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics cannot be null");
        this.plugins = List.copyOf(plugins);
    }

    /**
     * Performs enrichment and returns the enriched snapshot.
     *
     * @param classification the classification result
     * @return enriched snapshot with labels and properties
     */
    public EnrichedSnapshot enrich(PrimaryClassificationResult classification) {
        diagnostics.info("Starting enrichment phase...");

        // Run built-in behavioral pattern detection
        BehavioralPatternEnricher builtIn = new BehavioralPatternEnricher(graph);
        Map<String, Set<SemanticLabel>> methodLabels = builtIn.enrichMethods();
        Map<String, Set<SemanticLabel>> typeLabels = builtIn.enrichTypes();

        // Merge built-in labels
        Map<String, Set<SemanticLabel>> allLabels = new HashMap<>(methodLabels);
        for (var entry : typeLabels.entrySet()) {
            allLabels.merge(entry.getKey(), entry.getValue(), this::mergeLabels);
        }

        Map<String, Map<String, Object>> allProperties = new HashMap<>();

        // Run external plugins
        if (!plugins.isEmpty()) {
            diagnostics.info(String.format("Running %d enrichment plugin(s)...", plugins.size()));
            ArchitectureQuery query = new DefaultArchitectureQuery(graph);
            EnrichmentContext context = new EnrichmentContext(classification, query, diagnostics);

            for (EnrichmentPlugin plugin : plugins) {
                try {
                    diagnostics.info(String.format("Executing enrichment plugin: %s", plugin.id()));
                    EnrichmentContribution contribution = plugin.enrich(context);

                    // Merge labels
                    for (var entry : contribution.labels().entrySet()) {
                        allLabels.merge(entry.getKey(), entry.getValue(), this::mergeLabels);
                    }

                    // Merge properties
                    for (var entry : contribution.properties().entrySet()) {
                        allProperties.merge(entry.getKey(), entry.getValue(), this::mergeProperties);
                    }

                    diagnostics.info(String.format(
                            "Plugin %s contributed %d labels and %d properties",
                            plugin.id(),
                            contribution.labels().size(),
                            contribution.properties().size()));
                } catch (Exception e) {
                    diagnostics.error(String.format("Enrichment plugin %s failed", plugin.id()), e);
                }
            }
        }

        EnrichedSnapshot snapshot = new EnrichedSnapshot(classification, allLabels, allProperties);

        var stats = snapshot.stats();
        diagnostics.info(String.format(
                "Enrichment completed: %d identifiers labeled, %d total labels applied",
                stats.enrichedIdentifierCount(), stats.totalLabels()));

        return snapshot;
    }

    /**
     * Merges two sets of labels.
     */
    private Set<SemanticLabel> mergeLabels(Set<SemanticLabel> set1, Set<SemanticLabel> set2) {
        Set<SemanticLabel> merged = new HashSet<>(set1);
        merged.addAll(set2);
        return merged;
    }

    /**
     * Merges two property maps (later values override earlier ones).
     */
    private Map<String, Object> mergeProperties(Map<String, Object> map1, Map<String, Object> map2) {
        Map<String, Object> merged = new HashMap<>(map1);
        merged.putAll(map2); // Later contributions override
        return merged;
    }

    /**
     * Discovers enrichment plugins via ServiceLoader.
     */
    private List<EnrichmentPlugin> discoverPlugins() {
        List<EnrichmentPlugin> discovered = new ArrayList<>();
        ServiceLoader<EnrichmentPlugin> loader = ServiceLoader.load(EnrichmentPlugin.class);

        for (EnrichmentPlugin plugin : loader) {
            discovered.add(plugin);
            diagnostics.info(String.format("Discovered enrichment plugin: %s", plugin.id()));
        }

        return discovered;
    }

    /**
     * Returns the list of registered plugins (for testing/debugging).
     */
    public List<EnrichmentPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }
}
