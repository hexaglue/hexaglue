package io.hexaglue.core.engine;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationStatus;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.domain.DomainClassifier;
import io.hexaglue.core.classification.port.PortClassifier;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.core.ir.export.IrExporter;
import io.hexaglue.core.plugin.PluginExecutionResult;
import io.hexaglue.core.plugin.PluginExecutor;
import io.hexaglue.spi.ir.IrSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
 *   <li>Classify types (DomainClassifier, PortClassifier)</li>
 *   <li>Export to IR (IrExporter)</li>
 * </ol>
 */
final class DefaultHexaGlueEngine implements HexaGlueEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultHexaGlueEngine.class);

    private final SpoonFrontend frontend;
    private final GraphBuilder graphBuilder;
    private final DomainClassifier domainClassifier;
    private final PortClassifier portClassifier;
    private final IrExporter irExporter;

    DefaultHexaGlueEngine() {
        this.frontend = new SpoonFrontend();
        this.graphBuilder = new GraphBuilder(true); // compute derived edges
        this.domainClassifier = new DomainClassifier();
        this.portClassifier = new PortClassifier();
        this.irExporter = new IrExporter();
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
            int typeCount = (int) model.types().count();
            log.info("Semantic model built: {} types", typeCount);

            if (typeCount == 0) {
                diagnostics.add(
                        Diagnostic.warning("HG-ENGINE-001", "No types found in base package: " + config.basePackage()));
            }

            // Step 2: Build application graph (including derived edges)
            log.info("Building application graph");
            model = frontend.build(input); // re-build as types() consumes the stream
            GraphMetadata metadata = GraphMetadata.of(config.basePackage(), config.javaVersion(), (int)
                    model.types().count());
            model = frontend.build(input); // re-build again
            ApplicationGraph graph = graphBuilder.build(model, metadata);
            log.info("Application graph built: {} nodes, {} edges", graph.nodeCount(), graph.edgeCount());

            // Step 3: Classify all types
            log.info("Classifying types");
            List<ClassificationResult> classifications = classifyAll(graph);

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

    private List<ClassificationResult> classifyAll(ApplicationGraph graph) {
        GraphQuery query = graph.query();
        List<ClassificationResult> results = new ArrayList<>();

        for (TypeNode type : graph.typeNodes()) {
            // Try domain classification first
            ClassificationResult domainResult = domainClassifier.classify(type, query);
            if (domainResult.isClassified() || domainResult.status() == ClassificationStatus.CONFLICT) {
                results.add(domainResult);
                continue;
            }

            // Then try port classification
            ClassificationResult portResult = portClassifier.classify(type, query);
            if (portResult.isClassified() || portResult.status() == ClassificationStatus.CONFLICT) {
                results.add(portResult);
            }
            // Unclassified types are not added to results
        }

        return results;
    }
}
