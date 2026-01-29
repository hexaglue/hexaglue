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

import io.hexaglue.plugin.livingdoc.content.DomainContentSelector;
import io.hexaglue.plugin.livingdoc.content.PortContentSelector;
import io.hexaglue.plugin.livingdoc.markdown.MarkdownBuilder;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.plugin.livingdoc.renderer.DiagramRenderer;
import io.hexaglue.plugin.livingdoc.util.PluginVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates Mermaid diagrams for the architecture.
 *
 * <p>Uses three-layer architecture:
 * <ol>
 *   <li>ContentSelector - selects and transforms model data to documentation models</li>
 *   <li>DocumentationModel - immutable records representing documentation content</li>
 *   <li>Renderer - renders documentation models to Mermaid diagrams</li>
 * </ol>
 *
 * @since 4.0.0
 * @since 5.0.0 - Accepts shared selectors and configurable DiagramRenderer
 */
public final class DiagramGenerator {

    private final DomainContentSelector domainSelector;
    private final PortContentSelector portSelector;
    private final DiagramRenderer renderer;

    /**
     * Creates a generator using shared content selectors and renderer.
     *
     * @param domainSelector the shared domain content selector
     * @param portSelector the shared port content selector
     * @param renderer the diagram renderer (with configurable max properties)
     * @since 5.0.0
     */
    public DiagramGenerator(
            DomainContentSelector domainSelector, PortContentSelector portSelector, DiagramRenderer renderer) {
        this.domainSelector = domainSelector;
        this.portSelector = portSelector;
        this.renderer = renderer;
    }

    public String generate() {
        MarkdownBuilder md = new MarkdownBuilder()
                .h1("Architecture Diagrams")
                .paragraph(PluginVersion.generatorHeader())
                .link("Back to Overview", "README.md")
                .newline()
                .newline()
                .horizontalRule();

        // Domain Model Class Diagram
        generateDomainClassDiagram(md);

        // Aggregate Diagrams
        generateAggregateDiagrams(md);

        // Ports Flow Diagram
        generatePortsFlowDiagram(md);

        // Dependencies Diagram
        generateDependenciesDiagram(md);

        return md.build();
    }

    private void generateDomainClassDiagram(MarkdownBuilder md) {
        md.h2("Domain Model").paragraph("Class diagram showing domain types.");

        List<DomainTypeDoc> allTypes = domainSelector.selectAllTypes();
        md.raw(renderer.renderDomainClassDiagram(allTypes));
    }

    private void generateAggregateDiagrams(MarkdownBuilder md) {
        List<DomainTypeDoc> aggregates = domainSelector.selectAggregateRoots();
        if (aggregates.isEmpty()) {
            return;
        }

        md.h2("Aggregates").paragraph("Each aggregate root with its entities and value objects.");

        // Build a map of all types for lookups
        List<DomainTypeDoc> allTypes = domainSelector.selectAllTypes();
        Map<String, DomainTypeDoc> typeMap = new HashMap<>();
        for (DomainTypeDoc type : allTypes) {
            typeMap.put(type.debug().qualifiedName(), type);
        }

        for (DomainTypeDoc aggregate : aggregates) {
            md.h3(aggregate.name() + " Aggregate").raw(renderer.renderAggregateDiagram(aggregate, typeMap));
        }
    }

    private void generatePortsFlowDiagram(MarkdownBuilder md) {
        md.h2("Port Interactions").paragraph("Flow of interactions through the hexagonal architecture.");

        List<PortDoc> drivingPorts = portSelector.selectDrivingPorts();
        List<PortDoc> drivenPorts = portSelector.selectDrivenPorts();
        List<DomainTypeDoc> aggregates = domainSelector.selectAggregateRoots();

        md.raw(renderer.renderPortsFlowDiagram(drivingPorts, drivenPorts, aggregates));
    }

    private void generateDependenciesDiagram(MarkdownBuilder md) {
        md.h2("Dependencies").paragraph("Code dependencies in hexagonal architecture point toward the domain.");

        List<PortDoc> drivingPorts = portSelector.selectDrivingPorts();
        List<PortDoc> drivenPorts = portSelector.selectDrivenPorts();
        List<DomainTypeDoc> aggregates = domainSelector.selectAggregateRoots();

        md.raw(renderer.renderDependenciesDiagram(drivingPorts, drivenPorts, aggregates));
    }
}
