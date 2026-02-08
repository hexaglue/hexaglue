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

package io.hexaglue.plugin.livingdoc;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.ModuleIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.plugin.livingdoc.config.LivingDocConfig;
import io.hexaglue.plugin.livingdoc.content.BoundedContextDetector;
import io.hexaglue.plugin.livingdoc.content.DomainContentSelector;
import io.hexaglue.plugin.livingdoc.content.GlossaryBuilder;
import io.hexaglue.plugin.livingdoc.content.PortContentSelector;
import io.hexaglue.plugin.livingdoc.content.RelationshipEnricher;
import io.hexaglue.plugin.livingdoc.content.StructureBuilder;
import io.hexaglue.plugin.livingdoc.generator.DiagramGenerator;
import io.hexaglue.plugin.livingdoc.generator.DomainDocGenerator;
import io.hexaglue.plugin.livingdoc.generator.ModuleDocGenerator;
import io.hexaglue.plugin.livingdoc.generator.OverviewGenerator;
import io.hexaglue.plugin.livingdoc.generator.PortDocGenerator;
import io.hexaglue.plugin.livingdoc.model.BoundedContextDoc;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel;
import io.hexaglue.plugin.livingdoc.model.DocumentationModelFactory;
import io.hexaglue.plugin.livingdoc.model.GlossaryEntry;
import io.hexaglue.plugin.livingdoc.model.LivingDocDiagramSet;
import io.hexaglue.plugin.livingdoc.renderer.DiagramRenderer;
import io.hexaglue.plugin.livingdoc.renderer.IndexRenderer;
import io.hexaglue.spi.generation.ArtifactWriter;
import io.hexaglue.spi.generation.GeneratorContext;
import io.hexaglue.spi.generation.GeneratorPlugin;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginContext;
import java.util.List;
import java.util.Optional;

/**
 * Living Documentation plugin for HexaGlue.
 *
 * <p>Generates living documentation in Markdown format:
 * <ul>
 *   <li>Architecture overview (README.md)</li>
 *   <li>Domain model documentation</li>
 *   <li>Ports documentation (driving and driven)</li>
 *   <li>Mermaid diagrams</li>
 * </ul>
 *
 * <p>Configuration options:
 * <ul>
 *   <li>{@code outputDir} - output directory for documentation (default: "living-doc")</li>
 *   <li>{@code generateDiagrams} - whether to generate Mermaid diagrams (default: true)</li>
 *   <li>{@code maxPropertiesInDiagram} - max properties per class in diagrams (default: 5)</li>
 *   <li>{@code includeDebugSections} - whether to include debug sections (default: true)</li>
 * </ul>
 *
 * <p>This plugin requires a v4 {@code ArchitecturalModel}. The documentation models
 * use SPI classification types ({@code ElementKind}, {@code ConfidenceLevel}) for output.
 *
 * <h2>v4.1.0 Migration</h2>
 * <p>Since v4.1.0, this plugin supports the new {@link DomainIndex}, {@link PortIndex},
 * and {@link ClassificationReport} APIs for improved classification insights. When
 * available, the plugin logs additional information about classification quality.</p>
 *
 * <h2>v5.0.0 Architecture</h2>
 * <p>The plugin creates shared content selectors and a centralized configuration
 * to avoid redundant lookups and improve consistency across generators.</p>
 *
 * @since 4.0.0
 * @since 4.1.0 - Added support for new classification report logging
 * @since 5.0.0 - Shared selectors, centralized config, configurable diagram renderer
 */
public final class LivingDocPlugin implements GeneratorPlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.livingdoc";

    /**
     * The v4 architectural model, captured in execute() before generate().
     */
    private ArchitecturalModel archModel;

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    /**
     * Captures the v4 ArchitecturalModel from the context before delegating to generate().
     *
     * @param context the plugin context (may be an ArchModelPluginContext)
     * @since 4.0.0
     */
    @Override
    public void execute(PluginContext context) {
        this.archModel = context.model();
        GeneratorPlugin.super.execute(context);
    }

    @Override
    public void generate(GeneratorContext context) throws Exception {
        ArtifactWriter writer = context.writer();
        DiagnosticReporter diagnostics = context.diagnostics();

        // Require v4 ArchitecturalModel
        if (archModel == null) {
            diagnostics.error("v4 ArchitecturalModel is required for living documentation generation. "
                    + "Please ensure the model is available.");
            return;
        }

        // Build documentation model from ArchitecturalModel
        // Note: DocumentationModel uses SPI classification types (ElementKind, ConfidenceLevel)
        DocumentationModel docModel = DocumentationModelFactory.fromArchModel(archModel);

        if (docModel.isEmpty()) {
            diagnostics.info("No domain types or ports to document");
            return;
        }

        // Centralized configuration
        LivingDocConfig config = LivingDocConfig.from(context.config());

        diagnostics.info("Generating living documentation in: " + config.outputDir());

        // Shared content selectors (created once, used by multiple generators)
        DomainContentSelector domainSelector = new DomainContentSelector(archModel);
        PortContentSelector portSelector = new PortContentSelector(archModel);

        // Detect bounded contexts from package analysis
        BoundedContextDetector bcDetector = new BoundedContextDetector(archModel);
        List<BoundedContextDoc> boundedContexts = bcDetector.detectAll();

        // Relationship enricher for domain documentation
        RelationshipEnricher relationshipEnricher = new RelationshipEnricher(archModel.compositionIndex());

        // Shared diagram renderer with configurable max properties
        DiagramRenderer diagramRenderer = new DiagramRenderer(config.maxPropertiesInDiagram());

        // Generate all diagrams once if enabled
        LivingDocDiagramSet diagramSet = null;
        if (config.generateDiagrams()) {
            diagramSet = LivingDocDiagramSet.generate(
                    docModel, domainSelector, portSelector, diagramRenderer, boundedContexts);
        }

        // Glossary
        GlossaryBuilder glossaryBuilder = new GlossaryBuilder(archModel);
        List<GlossaryEntry> glossaryEntries = glossaryBuilder.buildAll();

        // Structure
        StructureBuilder structureBuilder = new StructureBuilder(archModel);
        String packageTree = structureBuilder.renderPackageTree();

        // Index renderer
        IndexRenderer indexRenderer = new IndexRenderer();

        // Extract module index for multi-module support (nullable in mono-module)
        ModuleIndex moduleIndex = archModel.moduleIndex().orElse(null);

        // Overview receives pre-rendered diagram (or null when diagrams disabled)
        String overviewDiagram = diagramSet != null ? diagramSet.architectureOverview() : null;
        OverviewGenerator overviewGen = new OverviewGenerator(
                docModel, overviewDiagram, boundedContexts, glossaryEntries, packageTree, indexRenderer, moduleIndex);
        String readme = overviewGen.generate();
        writer.writeDoc(config.outputDir() + "/README.md", readme);
        diagnostics.info("Generated architecture overview");

        // Generate modules documentation if multi-module
        if (moduleIndex != null) {
            ModuleDocGenerator moduleDocGen = new ModuleDocGenerator(moduleIndex);
            String modulesDoc = moduleDocGen.generate();
            writer.writeDoc(config.outputDir() + "/modules.md", modulesDoc);
            diagnostics.info("Generated modules documentation");
        }

        // Generate domain documentation
        DomainDocGenerator domainGen = new DomainDocGenerator(domainSelector, relationshipEnricher);
        String domainDoc = domainGen.generate();
        writer.writeDoc(config.outputDir() + "/domain.md", domainDoc);
        diagnostics.info("Generated domain model documentation");

        // Generate ports documentation
        PortDocGenerator portGen = new PortDocGenerator(portSelector);
        String portsDoc = portGen.generate();
        writer.writeDoc(config.outputDir() + "/ports.md", portsDoc);
        diagnostics.info("Generated ports documentation");

        // Generate diagrams file if enabled
        if (diagramSet != null) {
            DiagramGenerator diagramGen = new DiagramGenerator(diagramSet);
            String diagrams = diagramGen.generate();
            writer.writeDoc(config.outputDir() + "/diagrams.md", diagrams);
            diagnostics.info("Generated architecture diagrams");
        }

        // Summary
        diagnostics.info(String.format(
                "Living documentation complete: %d aggregate roots, %d entities, %d value objects, "
                        + "%d driving ports, %d driven ports",
                docModel.aggregateRoots().size(),
                docModel.entities().size(),
                docModel.valueObjects().size(),
                docModel.drivingPorts().size(),
                docModel.drivenPorts().size()));

        // v4.1.0: Log classification quality information if available
        logClassificationInfo(archModel, diagnostics);
    }

    /**
     * Logs classification information using the new v4.1.0 API if available.
     *
     * @param model the architectural model
     * @param diagnostics the diagnostic reporter
     * @since 4.1.0
     */
    private void logClassificationInfo(ArchitecturalModel model, DiagnosticReporter diagnostics) {
        Optional<DomainIndex> domainIndexOpt = model.domainIndex();
        Optional<ClassificationReport> reportOpt = model.classificationReport();

        // v4.1.0: Use new indices for type counts
        if (domainIndexOpt.isPresent()) {
            DomainIndex domain = domainIndexOpt.get();
            long idCount = domain.identifiers().count();
            long eventCount = domain.domainEvents().count();
            long serviceCount = domain.domainServices().count();

            if (idCount > 0 || eventCount > 0 || serviceCount > 0) {
                diagnostics.info(String.format(
                        "Additional types: %d identifiers, %d domain events, %d domain services",
                        idCount, eventCount, serviceCount));
            }
        }

        // v4.1.0: Log classification quality
        reportOpt.ifPresent(report -> {
            if (report.hasIssues()) {
                diagnostics.warn(String.format(
                        "Classification quality: %.1f%% (%d types may need review)",
                        report.stats().classificationRate() * 100,
                        report.actionRequired().size()));
            } else {
                diagnostics.info(String.format(
                        "Classification quality: %.1f%% (all types classified)",
                        report.stats().classificationRate() * 100));
            }
        });
    }
}
