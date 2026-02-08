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

package io.hexaglue.plugin.audit.adapter.diagram;

import io.hexaglue.plugin.audit.domain.model.report.ComponentDetails;
import io.hexaglue.plugin.audit.domain.model.report.DiagramSet;
import io.hexaglue.plugin.audit.domain.model.report.ReportData;
import java.util.Optional;

/**
 * Orchestrates the generation of all Mermaid diagrams for the audit report.
 *
 * <p>This class coordinates the individual diagram builders to produce
 * a complete {@link DiagramSet} that can be shared between HTML and
 * Markdown renderers, ensuring diagram consistency across formats.
 *
 * <h2>Diagrams Generated</h2>
 * <h3>Required (6 diagrams)</h3>
 * <ul>
 *   <li>{@code scoreRadar} - radar-beta chart for scores</li>
 *   <li>{@code c4Context} - C4Context diagram</li>
 *   <li>{@code c4Component} - C4Component diagram</li>
 *   <li>{@code domainModel} - classDiagram for domain model</li>
 *   <li>{@code violationsPie} - pie chart for violations</li>
 *   <li>{@code packageZones} - quadrantChart for package metrics</li>
 * </ul>
 *
 * <h3>Optional (since 5.0.0)</h3>
 * <ul>
 *   <li>{@code applicationLayer} - classDiagram for application services layer</li>
 *   <li>{@code portsLayer} - classDiagram for ports layer</li>
 *   <li>{@code fullArchitecture} - C4Component for full hexagonal architecture overview</li>
 * </ul>
 *
 * @since 5.0.0
 */
public class DiagramGenerator {

    private final RadarChartBuilder radarChartBuilder;
    private final C4ContextDiagramBuilder c4ContextBuilder;
    private final C4ComponentDiagramBuilder c4ComponentBuilder;
    private final ClassDiagramBuilder classDiagramBuilder;
    private final PieChartBuilder pieChartBuilder;
    private final QuadrantChartBuilder quadrantChartBuilder;
    private final ApplicationLayerDiagramBuilder applicationLayerBuilder;
    private final PortsDiagramBuilder portsDiagramBuilder;
    private final FullArchitectureDiagramBuilder fullArchitectureBuilder;
    private final ModuleTopologyDiagramBuilder moduleTopologyBuilder;

    /**
     * Creates a diagram generator with default builders.
     */
    public DiagramGenerator() {
        this(
                new RadarChartBuilder(),
                new C4ContextDiagramBuilder(),
                new C4ComponentDiagramBuilder(),
                new ClassDiagramBuilder(),
                new PieChartBuilder(),
                new QuadrantChartBuilder(),
                new ApplicationLayerDiagramBuilder(),
                new PortsDiagramBuilder(),
                new FullArchitectureDiagramBuilder(),
                new ModuleTopologyDiagramBuilder());
    }

    /**
     * Creates a diagram generator with custom builders.
     *
     * @param radarChartBuilder radar chart builder
     * @param c4ContextBuilder C4 context diagram builder
     * @param c4ComponentBuilder C4 component diagram builder
     * @param classDiagramBuilder class diagram builder
     * @param pieChartBuilder pie chart builder
     * @param quadrantChartBuilder quadrant chart builder
     * @param applicationLayerBuilder application layer diagram builder
     * @param portsDiagramBuilder ports layer diagram builder
     * @param fullArchitectureBuilder full architecture diagram builder
     * @param moduleTopologyBuilder module topology diagram builder
     * @since 5.0.0 added applicationLayerBuilder, portsDiagramBuilder, fullArchitectureBuilder, and moduleTopologyBuilder
     */
    public DiagramGenerator(
            RadarChartBuilder radarChartBuilder,
            C4ContextDiagramBuilder c4ContextBuilder,
            C4ComponentDiagramBuilder c4ComponentBuilder,
            ClassDiagramBuilder classDiagramBuilder,
            PieChartBuilder pieChartBuilder,
            QuadrantChartBuilder quadrantChartBuilder,
            ApplicationLayerDiagramBuilder applicationLayerBuilder,
            PortsDiagramBuilder portsDiagramBuilder,
            FullArchitectureDiagramBuilder fullArchitectureBuilder,
            ModuleTopologyDiagramBuilder moduleTopologyBuilder) {
        this.radarChartBuilder = radarChartBuilder;
        this.c4ContextBuilder = c4ContextBuilder;
        this.c4ComponentBuilder = c4ComponentBuilder;
        this.classDiagramBuilder = classDiagramBuilder;
        this.pieChartBuilder = pieChartBuilder;
        this.quadrantChartBuilder = quadrantChartBuilder;
        this.applicationLayerBuilder = applicationLayerBuilder;
        this.portsDiagramBuilder = portsDiagramBuilder;
        this.fullArchitectureBuilder = fullArchitectureBuilder;
        this.moduleTopologyBuilder = moduleTopologyBuilder;
    }

    /**
     * Creates a diagram generator with original builders (backward compatibility).
     *
     * @param radarChartBuilder radar chart builder
     * @param c4ContextBuilder C4 context diagram builder
     * @param c4ComponentBuilder C4 component diagram builder
     * @param classDiagramBuilder class diagram builder
     * @param pieChartBuilder pie chart builder
     * @param quadrantChartBuilder quadrant chart builder
     */
    public DiagramGenerator(
            RadarChartBuilder radarChartBuilder,
            C4ContextDiagramBuilder c4ContextBuilder,
            C4ComponentDiagramBuilder c4ComponentBuilder,
            ClassDiagramBuilder classDiagramBuilder,
            PieChartBuilder pieChartBuilder,
            QuadrantChartBuilder quadrantChartBuilder) {
        this(
                radarChartBuilder,
                c4ContextBuilder,
                c4ComponentBuilder,
                classDiagramBuilder,
                pieChartBuilder,
                quadrantChartBuilder,
                new ApplicationLayerDiagramBuilder(),
                new PortsDiagramBuilder(),
                new FullArchitectureDiagramBuilder(),
                new ModuleTopologyDiagramBuilder());
    }

    /**
     * Generates all diagrams for the audit report.
     *
     * @param reportData the report data containing all structured information
     * @param projectName the project name for diagram titles
     * @return complete diagram set with all required diagrams and optional layer diagrams
     */
    public DiagramSet generate(ReportData reportData, String projectName) {
        return DiagramSet.builder()
                .scoreRadar(generateScoreRadar(reportData))
                .c4Context(generateC4Context(reportData, projectName))
                .c4Component(generateC4Component(reportData, projectName))
                .domainModel(generateDomainModel(reportData))
                .aggregateDiagrams(generateAggregateDiagrams(reportData))
                .violationsPie(generateViolationsPie(reportData))
                .packageZones(generatePackageZones(reportData))
                .applicationLayer(generateApplicationLayer(reportData).orElse(null))
                .portsLayer(generatePortsLayer(reportData).orElse(null))
                .fullArchitecture(
                        generateFullArchitecture(reportData, projectName).orElse(null))
                .moduleTopology(generateModuleTopology(reportData).orElse(null))
                .build();
    }

    /**
     * Generates the score radar chart.
     */
    private String generateScoreRadar(ReportData reportData) {
        return radarChartBuilder.build(reportData.appendix().scoreBreakdown());
    }

    /**
     * Generates the C4 Context diagram.
     */
    private String generateC4Context(ReportData reportData, String projectName) {
        return c4ContextBuilder.build(
                projectName,
                reportData.architecture().components().drivingPorts(),
                reportData.architecture().components().drivenPorts());
    }

    /**
     * Generates the C4 Component diagram.
     */
    private String generateC4Component(ReportData reportData, String projectName) {
        return c4ComponentBuilder.build(
                projectName,
                reportData.architecture().components(),
                reportData.architecture().relationships());
    }

    /**
     * Generates the domain model class diagram.
     */
    private String generateDomainModel(ReportData reportData) {
        return classDiagramBuilder.build(
                reportData.architecture().components(),
                reportData.architecture().relationships(),
                reportData.architecture().typeViolations());
    }

    /**
     * Generates individual diagrams for each aggregate.
     * Currently returns empty list to use single combined domain model diagram.
     */
    private java.util.List<DiagramSet.AggregateDiagram> generateAggregateDiagrams(ReportData reportData) {
        // Return empty list - use combined domainModel diagram instead
        // Individual diagrams can cause issues with cross-aggregate references
        return java.util.List.of();
    }

    /**
     * Generates the violations pie chart.
     */
    private String generateViolationsPie(ReportData reportData) {
        return pieChartBuilder.build(reportData.issues().summary());
    }

    /**
     * Generates the package zones quadrant chart.
     */
    private String generatePackageZones(ReportData reportData) {
        return quadrantChartBuilder.build(reportData.appendix().packageMetrics());
    }

    /**
     * Generates the application layer class diagram.
     *
     * <p>Shows application services, command handlers, and query handlers
     * with their relationships to aggregates and ports.
     *
     * @param reportData the report data
     * @return Optional containing the Mermaid diagram, or empty if no application layer components
     * @since 5.0.0
     */
    private Optional<String> generateApplicationLayer(ReportData reportData) {
        ComponentDetails components = reportData.architecture().components();

        // Skip if no application layer components
        if (!components.hasApplicationLayer()) {
            return Optional.empty();
        }

        return applicationLayerBuilder.build(
                components,
                reportData.architecture().relationships(),
                reportData.architecture().typeViolations());
    }

    /**
     * Generates the ports layer class diagram.
     *
     * <p>Shows driving ports (inbound) and driven ports (outbound) with their
     * adapter implementations.
     *
     * @param reportData the report data
     * @return Optional containing the Mermaid diagram, or empty if no ports
     * @since 5.0.0
     */
    private Optional<String> generatePortsLayer(ReportData reportData) {
        ComponentDetails components = reportData.architecture().components();

        // Skip if no ports
        if (!components.hasPortsLayer()) {
            return Optional.empty();
        }

        return portsDiagramBuilder.build(components, reportData.architecture().typeViolations());
    }

    /**
     * Generates the full architecture C4Component diagram.
     *
     * <p>Shows the complete hexagonal architecture with all three layers:
     * Driving Side (application services, driving ports), Domain Core (aggregates),
     * and Driven Side (driven ports, adapters).
     *
     * @param reportData the report data
     * @param projectName the project name for the diagram title
     * @return Optional containing the Mermaid C4Component diagram, or empty if insufficient content
     * @since 5.0.0
     */
    private Optional<String> generateFullArchitecture(ReportData reportData, String projectName) {
        return fullArchitectureBuilder.build(
                projectName,
                reportData.architecture().components(),
                reportData.architecture().relationships(),
                reportData.architecture().typeViolations());
    }

    /**
     * Generates the module topology diagram.
     *
     * <p>Shows the modules in a multi-module project with their roles
     * and type counts.
     *
     * @param reportData the report data
     * @return Optional containing the Mermaid diagram, or empty if mono-module
     * @since 5.0.0
     */
    private Optional<String> generateModuleTopology(ReportData reportData) {
        return moduleTopologyBuilder.build(reportData.architecture().moduleTopology());
    }
}
