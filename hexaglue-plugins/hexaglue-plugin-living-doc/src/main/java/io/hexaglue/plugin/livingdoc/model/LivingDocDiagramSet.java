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

package io.hexaglue.plugin.livingdoc.model;

import io.hexaglue.plugin.livingdoc.content.DomainContentSelector;
import io.hexaglue.plugin.livingdoc.content.PortContentSelector;
import io.hexaglue.plugin.livingdoc.mermaid.GraphBuilder;
import io.hexaglue.plugin.livingdoc.mermaid.MermaidBuilder;
import io.hexaglue.plugin.livingdoc.renderer.DiagramRenderer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable set of all Mermaid diagrams for living documentation.
 *
 * <p>Centralizes diagram generation so that all diagrams are computed once
 * and shared across generators ({@code OverviewGenerator}, {@code DiagramGenerator}).
 *
 * <p>The factory method {@link #generate} orchestrates the creation of all five
 * diagram types using the provided content selectors and renderer.
 *
 * @param architectureOverview graph LR diagram showing hexagonal architecture overview
 * @param domainModel classDiagram showing all domain types
 * @param aggregateDiagrams per-aggregate classDiagrams keyed by aggregate name (insertion-ordered)
 * @param portsFlow flowchart LR showing port interactions
 * @param dependencyGraph flowchart TB showing dependency directions
 * @since 5.0.0
 */
public record LivingDocDiagramSet(
        String architectureOverview,
        String domainModel,
        Map<String, String> aggregateDiagrams,
        String portsFlow,
        String dependencyGraph) {

    /**
     * Compact constructor validating non-null fields and making aggregateDiagrams unmodifiable.
     */
    public LivingDocDiagramSet {
        Objects.requireNonNull(architectureOverview, "architectureOverview must not be null");
        Objects.requireNonNull(domainModel, "domainModel must not be null");
        Objects.requireNonNull(portsFlow, "portsFlow must not be null");
        Objects.requireNonNull(dependencyGraph, "dependencyGraph must not be null");
        aggregateDiagrams = Collections.unmodifiableMap(new LinkedHashMap<>(aggregateDiagrams));
    }

    /**
     * Generates all living documentation diagrams from the architectural model.
     *
     * <p>This factory method orchestrates the five diagram types:
     * <ol>
     *   <li>Architecture overview (graph LR) - hexagonal layers</li>
     *   <li>Domain model (classDiagram) - all domain types</li>
     *   <li>Aggregate diagrams (classDiagram per aggregate) - individual aggregate views</li>
     *   <li>Ports flow (flowchart LR) - interaction flow through ports</li>
     *   <li>Dependency graph (flowchart TB) - dependency direction toward domain</li>
     * </ol>
     *
     * @param docModel the documentation model for the overview diagram
     * @param domainSelector the domain content selector
     * @param portSelector the port content selector
     * @param renderer the diagram renderer
     * @return a fully populated diagram set
     * @since 5.0.0
     */
    public static LivingDocDiagramSet generate(
            DocumentationModel docModel,
            DomainContentSelector domainSelector,
            PortContentSelector portSelector,
            DiagramRenderer renderer) {
        return generate(docModel, domainSelector, portSelector, renderer, List.of());
    }

    /**
     * Generates all living documentation diagrams with bounded context information.
     *
     * <p>When more than one bounded context is provided, the dependency graph
     * uses subgraphs to group types by bounded context.
     *
     * @param docModel the documentation model for the overview diagram
     * @param domainSelector the domain content selector
     * @param portSelector the port content selector
     * @param renderer the diagram renderer
     * @param boundedContexts the detected bounded contexts
     * @return a fully populated diagram set
     * @since 5.0.0
     */
    public static LivingDocDiagramSet generate(
            DocumentationModel docModel,
            DomainContentSelector domainSelector,
            PortContentSelector portSelector,
            DiagramRenderer renderer,
            List<BoundedContextDoc> boundedContexts) {
        Objects.requireNonNull(docModel, "docModel must not be null");
        Objects.requireNonNull(domainSelector, "domainSelector must not be null");
        Objects.requireNonNull(portSelector, "portSelector must not be null");
        Objects.requireNonNull(renderer, "renderer must not be null");
        Objects.requireNonNull(boundedContexts, "boundedContexts must not be null");

        // 1. Architecture overview
        String architectureOverview = generateArchitectureOverview(docModel);

        // 2. Domain model class diagram
        List<DomainTypeDoc> allTypes = domainSelector.selectAllTypes();
        String domainModel = renderer.renderDomainClassDiagram(allTypes);

        // 3. Per-aggregate diagrams
        Map<String, String> aggregateDiagrams = generateAggregateDiagrams(domainSelector, renderer, allTypes);

        // 4. Ports flow diagram
        List<PortDoc> drivingPorts = portSelector.selectDrivingPorts();
        List<PortDoc> drivenPorts = portSelector.selectDrivenPorts();
        List<DomainTypeDoc> aggregates = domainSelector.selectAggregateRoots();
        List<DomainTypeDoc> appServices = domainSelector.selectApplicationServices();
        String portsFlow = renderer.renderPortsFlowDiagram(drivingPorts, drivenPorts, aggregates, appServices);

        // 5. Dependency graph (enhanced if multiple bounded contexts)
        String dependencyGraph = renderer.renderEnhancedDependenciesDiagram(
                drivingPorts, drivenPorts, aggregates, boundedContexts, appServices);

        return new LivingDocDiagramSet(
                architectureOverview, domainModel, aggregateDiagrams, portsFlow, dependencyGraph);
    }

    /**
     * Generates the architecture overview diagram using GraphBuilder.
     *
     * <p>This logic was previously in {@code OverviewGenerator.generateOverviewDiagram()}.
     */
    private static String generateArchitectureOverview(DocumentationModel model) {
        GraphBuilder graph = new GraphBuilder(GraphBuilder.Direction.LEFT_TO_RIGHT);

        // External actors
        graph.startSubgraph("External", "External Actors")
                .node("UI", "[UI/API]")
                .endSubgraph();

        // Application layer
        graph.startSubgraph("Application", "Application");

        // Driving Ports subgraph
        graph.startSubgraph("DrivingPorts", "Driving Ports");
        List<DocumentationModel.DocPort> driving = model.drivingPorts();
        if (driving.isEmpty()) {
            graph.node("DP", "[\"(none)\"]");
        } else {
            for (DocumentationModel.DocPort port : driving) {
                graph.node(MermaidBuilder.sanitizeId(port.simpleName()), "[\"" + port.simpleName() + "\"]");
            }
        }
        graph.endSubgraph();

        // Application Services subgraph (optional)
        List<DocumentationModel.DocType> appServices = model.applicationServices();
        if (!appServices.isEmpty()) {
            graph.startSubgraph("AppServices", "Application Services");
            for (DocumentationModel.DocType svc : appServices) {
                graph.node(MermaidBuilder.sanitizeId(svc.simpleName()), "[\"" + svc.simpleName() + "\"]");
            }
            graph.endSubgraph();
        }

        // Domain subgraph
        graph.startSubgraph("Domain", "Domain");
        List<DocumentationModel.DocType> aggregates = model.aggregateRoots();
        if (aggregates.isEmpty()) {
            graph.node("D", "[\"(domain)\"]");
        } else {
            for (DocumentationModel.DocType agg : aggregates) {
                graph.node(MermaidBuilder.sanitizeId(agg.simpleName()), "[\"" + agg.simpleName() + "\"]");
            }
        }
        graph.endSubgraph();

        // Driven Ports subgraph
        graph.startSubgraph("DrivenPorts", "Driven Ports");
        List<DocumentationModel.DocPort> driven = model.drivenPorts();
        if (driven.isEmpty()) {
            graph.node("DPOUT", "[\"(none)\"]");
        } else {
            for (DocumentationModel.DocPort port : driven) {
                graph.node(MermaidBuilder.sanitizeId(port.simpleName()), "[\"" + port.simpleName() + "\"]");
            }
        }
        graph.endSubgraph();

        graph.endSubgraph(); // End Application

        // Infrastructure
        graph.startSubgraph("Infrastructure", "Infrastructure")
                .node("DB", "[(Database)]")
                .node("EXT", "[External Services]")
                .endSubgraph();

        // Connections
        graph.arrow("UI", "DrivingPorts");
        if (!appServices.isEmpty()) {
            graph.arrow("DrivingPorts", "AppServices")
                    .arrow("AppServices", "Domain");
        } else {
            graph.arrow("DrivingPorts", "Domain");
        }
        graph.arrow("Domain", "DrivenPorts")
                .arrow("DrivenPorts", "DB")
                .arrow("DrivenPorts", "EXT");

        return graph.build();
    }

    /**
     * Generates per-aggregate class diagrams, preserving insertion order.
     */
    private static Map<String, String> generateAggregateDiagrams(
            DomainContentSelector domainSelector, DiagramRenderer renderer, List<DomainTypeDoc> allTypes) {
        List<DomainTypeDoc> aggregates = domainSelector.selectAggregateRoots();
        if (aggregates.isEmpty()) {
            return Map.of();
        }

        // Build type lookup map
        Map<String, DomainTypeDoc> typeMap = new HashMap<>();
        for (DomainTypeDoc type : allTypes) {
            typeMap.put(type.debug().qualifiedName(), type);
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (DomainTypeDoc aggregate : aggregates) {
            result.put(aggregate.name(), renderer.renderAggregateDiagram(aggregate, typeMap));
        }
        return result;
    }
}
