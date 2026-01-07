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

package io.hexaglue.core.engine;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationStatus;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.SinglePassClassifier;
import io.hexaglue.core.classification.engine.CriteriaProfile;
import io.hexaglue.core.classification.engine.YamlCriteriaProfile;
import io.hexaglue.core.frontend.JavaFrontend;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.ir.export.IrExporter;
import io.hexaglue.core.plugin.PluginExecutionResult;
import io.hexaglue.core.plugin.PluginExecutor;
import io.hexaglue.spi.ir.IrSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link HexaGlueEngine}.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Build Spoon model (SpoonFrontend)</li>
 *   <li>Convert to JavaSemanticModel</li>
 *   <li>Build ApplicationGraph (GraphBuilder)</li>
 *   <li>Derive edges (DerivedEdgeComputer)</li>
 *   <li>Classify types (SinglePassClassifier - ports first, then domain)</li>
 *   <li>Export to IR (IrExporter)</li>
 * </ol>
 */
public final class DefaultHexaGlueEngine implements HexaGlueEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultHexaGlueEngine.class);

    private final JavaFrontend frontend;
    private final GraphBuilder graphBuilder;
    private final SinglePassClassifier classifier;
    private final IrExporter irExporter;

    /**
     * Creates an engine with custom components (for testing or customization).
     *
     * <p>Use {@link #withDefaults()} for standard production configuration.
     *
     * @param frontend the Java frontend implementation
     * @param graphBuilder the graph builder
     * @param classifier the type classifier (can be null to use config-based profile)
     * @param irExporter the IR exporter
     */
    public DefaultHexaGlueEngine(
            JavaFrontend frontend, GraphBuilder graphBuilder, SinglePassClassifier classifier, IrExporter irExporter) {
        this.frontend = Objects.requireNonNull(frontend, "frontend cannot be null");
        this.graphBuilder = Objects.requireNonNull(graphBuilder, "graphBuilder cannot be null");
        this.classifier = classifier; // Can be null - will be created in analyze() with config profile
        this.irExporter = Objects.requireNonNull(irExporter, "irExporter cannot be null");
    }

    /**
     * Creates an engine with default components.
     *
     * <p>This is the standard configuration for production use.
     * The classifier will be created dynamically based on the config's classification profile.
     *
     * @return a new engine with default settings
     */
    public static DefaultHexaGlueEngine withDefaults() {
        return new DefaultHexaGlueEngine(
                new SpoonFrontend(),
                new GraphBuilder(true), // compute derived edges
                null, // classifier created dynamically based on config profile
                new IrExporter());
    }

    /**
     * Loads a CriteriaProfile from the given profile name.
     *
     * @param profileName the profile name (e.g., "default", "strict", "repository-aware")
     * @return the loaded profile, or legacy profile if name is null
     */
    private static CriteriaProfile loadProfile(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            log.debug("No classification profile specified, using legacy behavior");
            return CriteriaProfile.legacy();
        }

        String resourcePath = "profiles/" + profileName + ".yaml";
        try {
            CriteriaProfile profile = YamlCriteriaProfile.fromResource(resourcePath);
            log.info("Loaded classification profile: {}", profileName);
            return profile;
        } catch (IllegalArgumentException e) {
            log.warn("Classification profile '{}' not found, falling back to legacy behavior", profileName);
            return CriteriaProfile.legacy();
        }
    }

    @Override
    public EngineResult analyze(EngineConfig config) {
        Instant start = Instant.now();
        List<Diagnostic> diagnostics = new ArrayList<>();

        try {
            // Step 1: Build semantic model from source
            log.info("Building semantic model for base package: {}", config.basePackage());
            JavaAnalysisInput input = new JavaAnalysisInput(
                    config.sourceRoots(), config.classpathEntries(), config.javaVersion(), config.basePackage());

            JavaSemanticModel model = frontend.build(input);
            int typeCount = model.types().size();
            log.info("Semantic model built: {} types", typeCount);

            if (typeCount == 0) {
                diagnostics.add(
                        Diagnostic.warning("HG-ENGINE-001", "No types found in base package: " + config.basePackage()));
            }

            // Step 2: Build application graph (including derived edges)
            log.info("Building application graph");
            GraphMetadata metadata = GraphMetadata.of(config.basePackage(), config.javaVersion(), typeCount);
            ApplicationGraph graph = graphBuilder.build(model, metadata);
            log.info("Application graph built: {} nodes, {} edges", graph.nodeCount(), graph.edgeCount());

            // Step 3: Classify all types
            log.info("Classifying types");
            List<ClassificationResult> classifications = classifyAll(graph, config.classificationProfile());

            int classifiedDomain = (int) classifications.stream()
                    .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                    .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                    .count();
            int classifiedPorts = (int) classifications.stream()
                    .filter(c -> c.target() == ClassificationTarget.PORT)
                    .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                    .count();
            int conflicts = (int) classifications.stream()
                    .filter(c -> c.status() == ClassificationStatus.CONFLICT)
                    .count();

            log.info(
                    "Classification complete: {} domain types, {} ports, {} conflicts",
                    classifiedDomain,
                    classifiedPorts,
                    conflicts);

            // Add diagnostics for conflicts
            classifications.stream()
                    .filter(c -> c.status() == ClassificationStatus.CONFLICT)
                    .forEach(c -> {
                        TypeNode node = graph.typeNode(c.subjectId()).orElse(null);
                        String typeName = node != null
                                ? node.qualifiedName()
                                : c.subjectId().toString();
                        diagnostics.add(Diagnostic.warning(
                                "HG-ENGINE-002",
                                "Classification conflict for type: " + typeName + " - " + c.justification()));
                    });

            // Step 4: Export to IR
            log.info("Exporting to IR");
            IrSnapshot ir = irExporter.export(graph, classifications);

            // Step 5: Execute plugins (if enabled)
            PluginExecutionResult pluginResult = null;
            if (config.pluginsEnabled()) {
                log.info("Executing plugins");
                PluginExecutor executor = new PluginExecutor(config.outputDirectory(), config.pluginConfigs());
                pluginResult = executor.execute(ir);
                log.info(
                        "Plugins executed: {} plugins, {} files generated",
                        pluginResult.pluginCount(),
                        pluginResult.totalGeneratedFiles());
            }

            Duration elapsed = Duration.between(start, Instant.now());
            log.info("Analysis complete in {}ms", elapsed.toMillis());

            return new EngineResult(
                    ir,
                    diagnostics,
                    new EngineMetrics(graph.typeCount(), classifiedDomain + classifiedPorts, classifiedPorts, elapsed),
                    pluginResult);

        } catch (Exception e) {
            log.error("Analysis failed", e);
            diagnostics.add(Diagnostic.error("HG-ENGINE-100", "Analysis failed: " + e.getMessage()));

            Duration elapsed = Duration.between(start, Instant.now());
            return new EngineResult(
                    IrSnapshot.empty(config.basePackage()), diagnostics, new EngineMetrics(0, 0, 0, elapsed), null);
        }
    }

    private List<ClassificationResult> classifyAll(ApplicationGraph graph, String profileName) {
        // Determine which classifier to use
        SinglePassClassifier effectiveClassifier;
        if (classifier != null) {
            // Use the injected classifier (for testing)
            effectiveClassifier = classifier;
        } else {
            // Create classifier with the specified profile
            CriteriaProfile profile = loadProfile(profileName);
            effectiveClassifier = new SinglePassClassifier(profile);
        }

        // Use SinglePassClassifier for unified classification
        // This ensures ports are classified FIRST, then domain types with port context
        ClassificationResults results = effectiveClassifier.classify(graph);

        // Filter to only return classified or conflicting types
        return results.stream()
                .filter(c -> c.isClassified() || c.status() == ClassificationStatus.CONFLICT)
                .toList();
    }
}
