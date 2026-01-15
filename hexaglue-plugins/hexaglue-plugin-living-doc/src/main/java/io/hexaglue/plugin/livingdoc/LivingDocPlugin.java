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
import io.hexaglue.plugin.livingdoc.generator.DiagramGenerator;
import io.hexaglue.plugin.livingdoc.generator.DomainDocGenerator;
import io.hexaglue.plugin.livingdoc.generator.OverviewGenerator;
import io.hexaglue.plugin.livingdoc.generator.PortDocGenerator;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel;
import io.hexaglue.plugin.livingdoc.model.DocumentationModelFactory;
import io.hexaglue.spi.arch.PluginContexts;
import io.hexaglue.spi.generation.ArtifactWriter;
import io.hexaglue.spi.generation.GeneratorContext;
import io.hexaglue.spi.generation.GeneratorPlugin;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;

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
 * </ul>
 *
 * <p>This plugin supports both legacy {@code IrSnapshot} and v4 {@code ArchitecturalModel}.
 * When an ArchitecturalModel is available, it uses the new unified API for improved
 * classification information. Otherwise, it falls back to IrSnapshot.
 *
 * @since 4.0.0 - Migrated to support v4 ArchitecturalModel
 */
public final class LivingDocPlugin implements GeneratorPlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.livingdoc";

    /**
     * The v4 architectural model, captured in execute() before generate().
     * May be null if running in legacy mode.
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
        this.archModel = PluginContexts.getModel(context).orElse(null);
        GeneratorPlugin.super.execute(context);
    }

    @Override
    public void generate(GeneratorContext context) throws Exception {
        IrSnapshot ir = context.ir();
        PluginConfig config = context.config();
        ArtifactWriter writer = context.writer();
        DiagnosticReporter diagnostics = context.diagnostics();

        // Build documentation model from the best available source
        DocumentationModel docModel;
        String sourceInfo;
        if (archModel != null) {
            docModel = DocumentationModelFactory.fromArchModel(archModel);
            sourceInfo = "v4 ArchitecturalModel";
            diagnostics.info("Using v4 ArchitecturalModel for documentation generation");
        } else {
            docModel = DocumentationModelFactory.fromIrSnapshot(ir);
            sourceInfo = "legacy IrSnapshot";
            diagnostics.info("Using legacy IrSnapshot for documentation generation");
        }

        if (docModel.isEmpty()) {
            diagnostics.info("No domain types or ports to document");
            return;
        }

        // Configuration
        String outputDir = config.getString("outputDir", "living-doc");
        boolean generateDiagrams = config.getBoolean("generateDiagrams", true);

        diagnostics.info("Generating living documentation in: " + outputDir);

        // Generate README/overview using new DocumentationModel
        OverviewGenerator overviewGen = new OverviewGenerator(docModel);
        String readme = overviewGen.generate(generateDiagrams);
        writer.writeDoc(outputDir + "/README.md", readme);
        diagnostics.info("Generated architecture overview");

        // Generate domain documentation - prefer v4 model when available
        DomainDocGenerator domainGen =
                archModel != null ? new DomainDocGenerator(archModel) : new DomainDocGenerator(ir);
        String domainDoc = domainGen.generate();
        writer.writeDoc(outputDir + "/domain.md", domainDoc);
        diagnostics.info("Generated domain model documentation");

        // Generate ports documentation - prefer v4 model when available
        PortDocGenerator portGen = archModel != null ? new PortDocGenerator(archModel) : new PortDocGenerator(ir);
        String portsDoc = portGen.generate();
        writer.writeDoc(outputDir + "/ports.md", portsDoc);
        diagnostics.info("Generated ports documentation");

        // Generate diagrams if enabled - prefer v4 model when available
        if (generateDiagrams) {
            DiagramGenerator diagramGen =
                    archModel != null ? new DiagramGenerator(archModel) : new DiagramGenerator(ir);
            String diagrams = diagramGen.generate();
            writer.writeDoc(outputDir + "/diagrams.md", diagrams);
            diagnostics.info("Generated architecture diagrams");
        }

        // Summary
        diagnostics.info(String.format(
                "Living documentation complete (%s): %d aggregate roots, %d entities, %d value objects, "
                        + "%d driving ports, %d driven ports",
                sourceInfo,
                docModel.aggregateRoots().size(),
                docModel.entities().size(),
                docModel.valueObjects().size(),
                docModel.drivingPorts().size(),
                docModel.drivenPorts().size()));
    }
}
