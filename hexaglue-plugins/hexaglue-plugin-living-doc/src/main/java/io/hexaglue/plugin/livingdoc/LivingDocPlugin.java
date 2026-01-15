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
 * @since 4.0.0 - Added support for v4 ArchitecturalModel
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
     * <p>This override allows the plugin to access both the legacy IrSnapshot (via GeneratorContext)
     * and the new ArchitecturalModel for future migration.
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

        if (ir.isEmpty()) {
            diagnostics.info("No domain types or ports to document");
            return;
        }

        // Configuration
        String outputDir = config.getString("outputDir", "living-doc");
        boolean generateDiagrams = config.getBoolean("generateDiagrams", true);

        diagnostics.info("Generating living documentation in: " + outputDir);

        // Create generators
        OverviewGenerator overviewGen = new OverviewGenerator(ir);
        DomainDocGenerator domainGen = new DomainDocGenerator(ir);
        PortDocGenerator portGen = new PortDocGenerator(ir);

        // Generate README/overview
        String readme = overviewGen.generate(generateDiagrams);
        writer.writeDoc(outputDir + "/README.md", readme);
        diagnostics.info("Generated architecture overview");

        // Generate domain documentation
        String domainDoc = domainGen.generate();
        writer.writeDoc(outputDir + "/domain.md", domainDoc);
        diagnostics.info("Generated domain model documentation");

        // Generate ports documentation
        String portsDoc = portGen.generate();
        writer.writeDoc(outputDir + "/ports.md", portsDoc);
        diagnostics.info("Generated ports documentation");

        // Generate diagrams if enabled
        if (generateDiagrams) {
            DiagramGenerator diagramGen = new DiagramGenerator(ir);
            String diagrams = diagramGen.generate();
            writer.writeDoc(outputDir + "/diagrams.md", diagrams);
            diagnostics.info("Generated architecture diagrams");
        }

        int typeCount = ir.domain().types().size();
        int portCount = ir.ports().ports().size();
        diagnostics.info(String.format(
                "Living documentation complete: %d domain types, %d ports documented", typeCount, portCount));

        // Log v4 model summary if available
        if (archModel != null) {
            logV4ModelSummary(archModel, diagnostics);
        }
    }

    /**
     * Logs a summary of the v4 architectural model for informational purposes.
     *
     * @param model the architectural model
     * @param diagnostics the diagnostic reporter
     */
    private void logV4ModelSummary(ArchitecturalModel model, DiagnosticReporter diagnostics) {
        long aggregateCount = model.aggregates().count();
        long entityCount = model.domainEntities().filter(e -> !e.isAggregateRoot()).count();
        long valueObjectCount = model.valueObjects().count();
        long drivingPortCount = model.drivingPorts().count();
        long drivenPortCount = model.drivenPorts().count();
        diagnostics.info(String.format(
                "v4 Model: %d aggregates, %d entities, %d value objects, %d driving ports, %d driven ports",
                aggregateCount, entityCount, valueObjectCount, drivingPortCount, drivenPortCount));
    }
}
