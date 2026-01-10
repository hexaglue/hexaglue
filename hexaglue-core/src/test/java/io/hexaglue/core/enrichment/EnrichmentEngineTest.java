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

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.enrichment.EnrichmentContext;
import io.hexaglue.spi.enrichment.EnrichmentContribution;
import io.hexaglue.spi.enrichment.EnrichmentPlugin;
import io.hexaglue.spi.enrichment.SemanticLabel;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrichmentEngineTest {

    private ApplicationGraph graph;
    private TestDiagnosticReporter diagnostics;
    private PrimaryClassificationResult classification;

    @BeforeEach
    void setUp() {
        GraphMetadata metadata = GraphMetadata.of("com.example", 17, 0);
        graph = new ApplicationGraph(metadata);
        diagnostics = new TestDiagnosticReporter();

        // Create a minimal classification result for testing
        classification = PrimaryClassificationResult.unclassified("com.example.DummyType", "Test classification");
    }

    @Test
    void shouldRunBuiltInEnricher() {
        EnrichmentEngine engine = new EnrichmentEngine(graph, diagnostics, List.of());

        EnrichedSnapshot snapshot = engine.enrich(List.of(classification));

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.classificationCount()).isEqualTo(1);
        assertThat(snapshot.classificationFor(classification.typeName())).contains(classification);
    }

    @Test
    void shouldMergePluginContributions() {
        TestEnrichmentPlugin plugin = new TestEnrichmentPlugin(
                "test.plugin",
                Map.of("Order", Set.of(SemanticLabel.AGGREGATE_BOUNDARY)),
                Map.of("Order", Map.of("complexity", 10)));

        EnrichmentEngine engine = new EnrichmentEngine(graph, diagnostics, List.of(plugin));

        EnrichedSnapshot snapshot = engine.enrich(List.of(classification));

        assertThat(snapshot.hasLabel("Order", SemanticLabel.AGGREGATE_BOUNDARY)).isTrue();
        assertThat(snapshot.property("Order", "complexity")).contains(10);
    }

    @Test
    void shouldMergeMultiplePluginContributions() {
        TestEnrichmentPlugin plugin1 = new TestEnrichmentPlugin(
                "plugin1", Map.of("Order", Set.of(SemanticLabel.AGGREGATE_BOUNDARY)), Map.of());

        TestEnrichmentPlugin plugin2 =
                new TestEnrichmentPlugin("plugin2", Map.of("Order", Set.of(SemanticLabel.EVENT_PUBLISHER)), Map.of());

        EnrichmentEngine engine = new EnrichmentEngine(graph, diagnostics, List.of(plugin1, plugin2));

        EnrichedSnapshot snapshot = engine.enrich(List.of(classification));

        assertThat(snapshot.labelsFor("Order"))
                .contains(SemanticLabel.AGGREGATE_BOUNDARY, SemanticLabel.EVENT_PUBLISHER);
    }

    @Test
    void shouldHandlePluginErrors() {
        FailingEnrichmentPlugin failingPlugin = new FailingEnrichmentPlugin();

        EnrichmentEngine engine = new EnrichmentEngine(graph, diagnostics, List.of(failingPlugin));

        EnrichedSnapshot snapshot = engine.enrich(List.of(classification));

        assertThat(snapshot).isNotNull();
        assertThat(diagnostics.errors).isNotEmpty();
    }

    // === Test utilities ===

    private static class TestEnrichmentPlugin implements EnrichmentPlugin {
        private final String id;
        private final Map<String, Set<SemanticLabel>> labels;
        private final Map<String, Map<String, Object>> properties;

        TestEnrichmentPlugin(
                String id, Map<String, Set<SemanticLabel>> labels, Map<String, Map<String, Object>> properties) {
            this.id = id;
            this.labels = labels;
            this.properties = properties;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public EnrichmentContribution enrich(EnrichmentContext context) {
            return new EnrichmentContribution(id, labels, properties);
        }

        @Override
        public void execute(io.hexaglue.spi.plugin.PluginContext context) {
            // Not used in enrichment
        }
    }

    private static class FailingEnrichmentPlugin implements EnrichmentPlugin {
        @Override
        public String id() {
            return "failing.plugin";
        }

        @Override
        public EnrichmentContribution enrich(EnrichmentContext context) {
            throw new RuntimeException("Simulated plugin failure");
        }

        @Override
        public void execute(io.hexaglue.spi.plugin.PluginContext context) {
            // Not used
        }
    }

    private static class TestDiagnosticReporter implements DiagnosticReporter {
        List<String> infos = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();

        @Override
        public void info(String message) {
            infos.add(message);
        }

        @Override
        public void warn(String message) {
            warnings.add(message);
        }

        @Override
        public void error(String message) {
            errors.add(message);
        }

        @Override
        public void error(String message, Throwable cause) {
            errors.add(message + ": " + cause.getMessage());
        }
    }
}
