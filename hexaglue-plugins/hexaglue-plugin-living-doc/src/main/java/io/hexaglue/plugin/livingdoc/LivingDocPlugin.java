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

import io.hexaglue.plugin.livingdoc.generator.DiagramGenerator;
import io.hexaglue.plugin.livingdoc.generator.DomainDocGenerator;
import io.hexaglue.plugin.livingdoc.generator.OverviewGenerator;
import io.hexaglue.plugin.livingdoc.generator.PortDocGenerator;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.plugin.CodeWriter;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import java.io.IOException;

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
 *   <li>{@code outputDir} - output directory for documentation (default: "docs/architecture")</li>
 *   <li>{@code generateDiagrams} - whether to generate Mermaid diagrams (default: true)</li>
 * </ul>
 */
public final class LivingDocPlugin implements HexaGluePlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.livingdoc";

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    @Override
    public void execute(PluginContext context) {
        IrSnapshot ir = context.ir();
        PluginConfig config = context.config();
        CodeWriter writer = context.writer();
        DiagnosticReporter diagnostics = context.diagnostics();

        if (ir.isEmpty()) {
            diagnostics.info("No domain types or ports to document");
            return;
        }

        // Configuration
        String outputDir = config.getString("outputDir", "docs/architecture");
        boolean generateDiagrams = config.getBoolean("generateDiagrams", true);

        diagnostics.info("Generating living documentation in: " + outputDir);

        // Create generators
        OverviewGenerator overviewGen = new OverviewGenerator(ir);
        DomainDocGenerator domainGen = new DomainDocGenerator(ir);
        PortDocGenerator portGen = new PortDocGenerator(ir);

        try {
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

        } catch (IOException e) {
            diagnostics.error("Failed to generate documentation", e);
        }
    }
}
