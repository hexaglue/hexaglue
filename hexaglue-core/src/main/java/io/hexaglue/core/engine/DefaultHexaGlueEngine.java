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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.builder.ArchitecturalModelBuilder;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationStatus;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.SinglePassClassifier;
import io.hexaglue.core.frontend.JavaFrontend;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.plugin.PluginExecutionResult;
import io.hexaglue.core.plugin.PluginExecutor;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.syntax.SyntaxProvider;
import io.hexaglue.syntax.spoon.SpoonSyntaxProvider;
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
 *   <li>Export primary classifications for plugin access</li>
 *   <li>Execute plugins (if enabled):
 *     <ul>
 *       <li>ANALYSIS plugins can implement secondary classification</li>
 *       <li>ENRICHMENT plugins can add semantic labels and metadata</li>
 *       <li>GENERATOR plugins generate code artifacts</li>
 *       <li>AUDIT plugins perform architecture compliance checks</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><b>Note on Secondary Classification and Enrichment:</b>
 * <p>Unlike the primary classification which is built into the engine pipeline,
 * secondary classification and enrichment are implemented as plugins:
 * <ul>
 *   <li><b>Secondary classifiers</b> are ANALYSIS plugins that can refine or override
 *       primary classifications by accessing primary results via the PluginOutputStore</li>
 *   <li><b>Enrichment</b> is performed by ENRICHMENT plugins that add semantic labels
 *       (factory methods, immutability, etc.) without generating code artifacts</li>
 * </ul>
 *
 * <p>This design keeps the core engine focused on deterministic classification while
 * allowing custom analysis and enrichment through the plugin ecosystem.
 */
public final class DefaultHexaGlueEngine implements HexaGlueEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultHexaGlueEngine.class);

    private final JavaFrontend frontend;
    private final GraphBuilder graphBuilder;
    private final SinglePassClassifier classifier;

    /**
     * Creates an engine with custom components (for testing or customization).
     *
     * <p>Use {@link #withDefaults()} for standard production configuration.
     *
     * @param frontend the Java frontend implementation
     * @param graphBuilder the graph builder
     * @param classifier the type classifier (can be null to use config-based profile)
     * @since 4.0.0 - Removed IrExporter, uses ArchitecturalModel exclusively
     */
    public DefaultHexaGlueEngine(JavaFrontend frontend, GraphBuilder graphBuilder, SinglePassClassifier classifier) {
        this.frontend = Objects.requireNonNull(frontend, "frontend cannot be null");
        this.graphBuilder = Objects.requireNonNull(graphBuilder, "graphBuilder cannot be null");
        this.classifier = classifier; // Can be null - will be created in analyze() with config profile
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
                null); // classifier created dynamically based on config profile
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
            List<ClassificationResult> classifications = classifyAll(graph, config);

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

            // Step 4: Build ArchitecturalModel
            log.info("Building ArchitecturalModel");
            ArchitecturalModel archModel = buildArchitecturalModel(config);
            log.debug("Built ArchitecturalModel with {} elements", archModel.size());

            // Step 4.5: Export primary classifications for enrichment and secondary classifiers
            // These classifications are made available to plugins via the PluginOutputStore
            List<PrimaryClassificationResult> primaryClassifications = exportPrimaryClassifications(classifications);
            log.debug("Exported {} primary classifications for plugin access", primaryClassifications.size());

            // Step 5: Execute plugins (if enabled)
            // Plugins of category ANALYSIS can implement secondary classification
            // Plugins of category ENRICHMENT can add semantic labels and metadata
            PluginExecutionResult pluginResult = null;
            if (config.pluginsEnabled()) {
                log.info("Executing plugins");
                PluginExecutor executor = new PluginExecutor(
                        config.outputDirectory(), config.pluginConfigs(), graph, config.enabledCategories(), archModel);
                pluginResult = executor.execute(primaryClassifications);
                log.info(
                        "Plugins executed: {} plugins, {} files generated",
                        pluginResult.pluginCount(),
                        pluginResult.totalGeneratedFiles());
            }

            Duration elapsed = Duration.between(start, Instant.now());
            log.info("Analysis complete in {}ms", elapsed.toMillis());

            return new EngineResult(
                    archModel,
                    diagnostics,
                    new EngineMetrics(graph.typeCount(), classifiedDomain + classifiedPorts, classifiedPorts, elapsed),
                    pluginResult,
                    primaryClassifications);

        } catch (Exception e) {
            log.error("Analysis failed", e);
            diagnostics.add(Diagnostic.error("HG-ENGINE-100", "Analysis failed: " + e.getMessage()));

            Duration elapsed = Duration.between(start, Instant.now());
            return new EngineResult(
                    buildEmptyModel(config), diagnostics, new EngineMetrics(0, 0, 0, elapsed), null, List.of());
        }
    }

    private List<ClassificationResult> classifyAll(ApplicationGraph graph, EngineConfig config) {
        // Determine which classifier to use
        SinglePassClassifier effectiveClassifier = classifier != null ? classifier : new SinglePassClassifier();

        // Use SinglePassClassifier for unified classification
        // This ensures ports are classified FIRST, then domain types with port context
        // Pass the classification config for user-defined exclusions and explicit classifications
        ClassificationResults results = effectiveClassifier.classify(graph, config.classificationConfig());

        // Filter to only return classified or conflicting types
        return results.stream()
                .filter(c -> c.isClassified() || c.status() == ClassificationStatus.CONFLICT)
                .toList();
    }

    /**
     * Builds the v4 ArchitecturalModel using SpoonSyntaxProvider and ArchitecturalModelBuilder.
     *
     * <p>This creates the unified model that plugins can access via ArchModelPluginContext.
     * The model uses the v4 classification system with TypeSyntax support.
     *
     * @param config the engine configuration
     * @return the built ArchitecturalModel
     * @since 4.0.0
     */
    private ArchitecturalModel buildArchitecturalModel(EngineConfig config) {
        // Build SyntaxProvider with the same sources as the main pipeline
        SyntaxProvider syntaxProvider = SpoonSyntaxProvider.builder()
                .basePackage(config.basePackage())
                .sourceDirectories(config.sourceRoots())
                .javaVersion(config.javaVersion())
                .build();

        // Build ArchitecturalModel using the v4 builder
        return ArchitecturalModelBuilder.builder(syntaxProvider)
                .projectName(config.projectName())
                .basePackage(config.basePackage())
                .build();
    }

    /**
     * Exports primary classifications for enrichment and secondary classifiers.
     *
     * <p>These classifications are made available to plugins via the PluginOutputStore.
     *
     * @param classifications the classification results
     * @return the primary classification results for plugin access
     * @since 4.0.0
     */
    private List<PrimaryClassificationResult> exportPrimaryClassifications(List<ClassificationResult> classifications) {
        return classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN
                        || (c.target() == null && c.status() == ClassificationStatus.UNCLASSIFIED))
                .map(this::toPrimaryClassificationResult)
                .sorted(java.util.Comparator.comparing(PrimaryClassificationResult::typeName))
                .toList();
    }

    /**
     * Converts a core ClassificationResult to an SPI PrimaryClassificationResult.
     */
    private PrimaryClassificationResult toPrimaryClassificationResult(ClassificationResult coreResult) {
        // Extract the qualified type name from NodeId (format: "type:com.example.Order")
        String nodeIdValue = coreResult.subjectId().value();
        String typeName = nodeIdValue.startsWith("type:") ? nodeIdValue.substring(5) : nodeIdValue;

        // Handle unclassified case
        if (coreResult.status() == ClassificationStatus.UNCLASSIFIED) {
            return PrimaryClassificationResult.unclassified(
                    typeName, coreResult.justification() != null ? coreResult.justification() : "No criteria matched");
        }

        // Handle conflict case - mark as uncertain
        if (coreResult.status() == ClassificationStatus.CONFLICT) {
            return new PrimaryClassificationResult(
                    typeName,
                    null, // kind is null for conflicts
                    io.hexaglue.spi.classification.CertaintyLevel.UNCERTAIN,
                    io.hexaglue.spi.classification.ClassificationStrategy.WEIGHTED,
                    coreResult.justification() != null ? coreResult.justification() : "Multiple conflicting criteria",
                    List.of());
        }

        // Handle successful classification
        io.hexaglue.arch.ElementKind elementKind = toElementKind(coreResult.kind());
        io.hexaglue.spi.classification.CertaintyLevel certainty = toSpiCertainty(coreResult.confidence());
        io.hexaglue.spi.classification.ClassificationStrategy strategy = deriveStrategy(coreResult);

        return new PrimaryClassificationResult(
                typeName,
                elementKind,
                certainty,
                strategy,
                coreResult.justification() != null ? coreResult.justification() : "",
                List.of());
    }

    /**
     * Converts kind string to ElementKind.
     */
    private io.hexaglue.arch.ElementKind toElementKind(String kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case "AGGREGATE_ROOT" -> io.hexaglue.arch.ElementKind.AGGREGATE_ROOT;
            case "ENTITY" -> io.hexaglue.arch.ElementKind.ENTITY;
            case "VALUE_OBJECT" -> io.hexaglue.arch.ElementKind.VALUE_OBJECT;
            case "IDENTIFIER" -> io.hexaglue.arch.ElementKind.IDENTIFIER;
            case "DOMAIN_EVENT" -> io.hexaglue.arch.ElementKind.DOMAIN_EVENT;
            case "DOMAIN_SERVICE" -> io.hexaglue.arch.ElementKind.DOMAIN_SERVICE;
            case "APPLICATION_SERVICE" -> io.hexaglue.arch.ElementKind.APPLICATION_SERVICE;
            case "INBOUND_ONLY" -> io.hexaglue.arch.ElementKind.INBOUND_ONLY;
            case "OUTBOUND_ONLY" -> io.hexaglue.arch.ElementKind.OUTBOUND_ONLY;
            case "SAGA" -> io.hexaglue.arch.ElementKind.SAGA;
            case "UNCLASSIFIED" -> io.hexaglue.arch.ElementKind.UNCLASSIFIED;
            default -> null;
        };
    }

    /**
     * Converts core confidence to SPI certainty level.
     */
    private io.hexaglue.spi.classification.CertaintyLevel toSpiCertainty(ConfidenceLevel confidence) {
        if (confidence == null) {
            return io.hexaglue.spi.classification.CertaintyLevel.NONE;
        }
        return switch (confidence) {
            case EXPLICIT -> io.hexaglue.spi.classification.CertaintyLevel.EXPLICIT;
            case HIGH -> io.hexaglue.spi.classification.CertaintyLevel.CERTAIN_BY_STRUCTURE;
            case MEDIUM -> io.hexaglue.spi.classification.CertaintyLevel.INFERRED;
            case LOW -> io.hexaglue.spi.classification.CertaintyLevel.UNCERTAIN;
        };
    }

    /**
     * Derives the classification strategy from the core result.
     */
    private io.hexaglue.spi.classification.ClassificationStrategy deriveStrategy(ClassificationResult result) {
        if (result.matchedCriteria() == null) {
            return io.hexaglue.spi.classification.ClassificationStrategy.UNCLASSIFIED;
        }

        String criteriaName = result.matchedCriteria().toLowerCase();

        if (criteriaName.contains("annotation") || criteriaName.contains("@")) {
            return io.hexaglue.spi.classification.ClassificationStrategy.ANNOTATION;
        }
        if (criteriaName.contains("repository")) {
            return io.hexaglue.spi.classification.ClassificationStrategy.REPOSITORY;
        }
        if (criteriaName.contains("record")) {
            return io.hexaglue.spi.classification.ClassificationStrategy.RECORD;
        }
        if (criteriaName.contains("composition")
                || criteriaName.contains("relationship")
                || criteriaName.contains("embedded")) {
            return io.hexaglue.spi.classification.ClassificationStrategy.COMPOSITION;
        }

        return io.hexaglue.spi.classification.ClassificationStrategy.WEIGHTED;
    }

    /**
     * Builds an empty ArchitecturalModel for error cases.
     */
    private ArchitecturalModel buildEmptyModel(EngineConfig config) {
        return ArchitecturalModel.builder(io.hexaglue.arch.ProjectContext.forTesting(
                        config.projectName() != null ? config.projectName() : "unknown", config.basePackage()))
                .build();
    }
}
