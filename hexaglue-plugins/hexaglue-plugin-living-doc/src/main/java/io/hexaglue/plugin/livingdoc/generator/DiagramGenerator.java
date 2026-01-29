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

package io.hexaglue.plugin.livingdoc.generator;

import io.hexaglue.plugin.livingdoc.markdown.MarkdownBuilder;
import io.hexaglue.plugin.livingdoc.model.LivingDocDiagramSet;
import io.hexaglue.plugin.livingdoc.util.PluginVersion;
import java.util.Map;
import java.util.Objects;

/**
 * Assembles pre-generated Mermaid diagrams into a Markdown document.
 *
 * <p>Since 5.0.0, this generator is a pure Markdown assembler that receives
 * all diagrams pre-rendered from {@link LivingDocDiagramSet}, rather than
 * generating them internally. This eliminates redundant diagram computation
 * when diagrams are shared across multiple generators.
 *
 * @since 4.0.0
 * @since 5.0.0 - Simplified to pure Markdown assembler using LivingDocDiagramSet
 */
public final class DiagramGenerator {

    private final LivingDocDiagramSet diagramSet;

    /**
     * Creates a generator using a pre-computed diagram set.
     *
     * @param diagramSet the shared diagram set containing all pre-rendered diagrams
     * @since 5.0.0
     */
    public DiagramGenerator(LivingDocDiagramSet diagramSet) {
        this.diagramSet = Objects.requireNonNull(diagramSet, "diagramSet must not be null");
    }

    /**
     * Assembles all diagrams into a single Markdown document.
     *
     * @return the generated Markdown content
     */
    public String generate() {
        MarkdownBuilder md = new MarkdownBuilder()
                .h1("Architecture Diagrams")
                .paragraph(PluginVersion.generatorHeader())
                .link("Back to Overview", "README.md")
                .newline()
                .newline()
                .horizontalRule();

        // Domain Model Class Diagram
        md.h2("Domain Model").paragraph("Class diagram showing domain types.");
        md.raw(diagramSet.domainModel());

        // Aggregate Diagrams
        if (!diagramSet.aggregateDiagrams().isEmpty()) {
            md.h2("Aggregates").paragraph("Each aggregate root with its entities and value objects.");
            for (Map.Entry<String, String> entry :
                    diagramSet.aggregateDiagrams().entrySet()) {
                md.h3(entry.getKey() + " Aggregate").raw(entry.getValue());
            }
        }

        // Ports Flow Diagram
        md.h2("Port Interactions").paragraph("Flow of interactions through the hexagonal architecture.");
        md.raw(diagramSet.portsFlow());

        // Dependencies Diagram
        md.h2("Dependencies").paragraph("Code dependencies in hexagonal architecture point toward the domain.");
        md.raw(diagramSet.dependencyGraph());

        return md.build();
    }
}
