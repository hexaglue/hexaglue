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

import io.hexaglue.plugin.audit.domain.model.report.DiagramSet;
import io.hexaglue.plugin.audit.domain.model.report.ReportData;

/**
 * Orchestrates the generation of all Mermaid diagrams for the audit report.
 *
 * <p>This class coordinates the individual diagram builders to produce
 * a complete {@link DiagramSet} that can be shared between HTML and
 * Markdown renderers, ensuring diagram consistency across formats.
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
                new QuadrantChartBuilder());
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
     */
    public DiagramGenerator(
            RadarChartBuilder radarChartBuilder,
            C4ContextDiagramBuilder c4ContextBuilder,
            C4ComponentDiagramBuilder c4ComponentBuilder,
            ClassDiagramBuilder classDiagramBuilder,
            PieChartBuilder pieChartBuilder,
            QuadrantChartBuilder quadrantChartBuilder) {
        this.radarChartBuilder = radarChartBuilder;
        this.c4ContextBuilder = c4ContextBuilder;
        this.c4ComponentBuilder = c4ComponentBuilder;
        this.classDiagramBuilder = classDiagramBuilder;
        this.pieChartBuilder = pieChartBuilder;
        this.quadrantChartBuilder = quadrantChartBuilder;
    }

    /**
     * Generates all diagrams for the audit report.
     *
     * @param reportData the report data containing all structured information
     * @param projectName the project name for diagram titles
     * @return complete diagram set with all 6 required diagrams
     */
    public DiagramSet generate(ReportData reportData, String projectName) {
        return DiagramSet.builder()
                .scoreRadar(generateScoreRadar(reportData))
                .c4Context(generateC4Context(reportData, projectName))
                .c4Component(generateC4Component(reportData, projectName))
                .domainModel(generateDomainModel(reportData))
                .violationsPie(generateViolationsPie(reportData))
                .packageZones(generatePackageZones(reportData))
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
                reportData.architecture().components(), reportData.architecture().relationships());
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
}
