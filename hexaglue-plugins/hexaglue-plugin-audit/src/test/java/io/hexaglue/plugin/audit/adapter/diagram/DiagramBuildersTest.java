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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.report.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Mermaid diagram builders.
 */
class DiagramBuildersTest {

    @Nested
    @DisplayName("RadarChartBuilder")
    class RadarChartBuilderTests {

        private final RadarChartBuilder builder = new RadarChartBuilder();

        @Test
        @DisplayName("should generate radar-beta chart with correct syntax")
        void shouldGenerateRadarBetaChart() {
            // Given
            var breakdown = createScoreBreakdown(78, 60, 100, 24, 100);

            // When
            String diagram = builder.build(breakdown);

            // Then
            assertThat(diagram).contains("radar-beta");
            assertThat(diagram).contains("title Architecture Compliance by Dimension");
            assertThat(diagram).contains("max 100");
            assertThat(diagram).contains("axis ddd");
            assertThat(diagram).contains("curve target");
            assertThat(diagram).contains("curve score");
            assertThat(diagram).contains("{78, 60, 100, 24, 100}");
        }

        @Test
        @DisplayName("should include configuration header")
        void shouldIncludeConfigHeader() {
            // Given
            var breakdown = createScoreBreakdown(80, 80, 80, 80, 80);

            // When
            String diagram = builder.build(breakdown);

            // Then
            assertThat(diagram).contains("---");
            assertThat(diagram).contains("config:");
            assertThat(diagram).contains("curveTension:");
        }
    }

    @Nested
    @DisplayName("PieChartBuilder")
    class PieChartBuilderTests {

        private final PieChartBuilder builder = new PieChartBuilder();

        @Test
        @DisplayName("should generate pie chart with all severities")
        void shouldGeneratePieChartWithAllSeverities() {
            // Given
            var counts = new ViolationCounts(15, 1, 2, 9, 2, 1);

            // When
            String diagram = builder.build(counts);

            // Then
            assertThat(diagram).contains("pie showData title Violations by Severity");
            assertThat(diagram).contains("\"BLOCKER\" : 1");
            assertThat(diagram).contains("\"CRITICAL\" : 2");
            assertThat(diagram).contains("\"MAJOR\" : 9");
            assertThat(diagram).contains("\"MINOR\" : 2");
            assertThat(diagram).contains("\"INFO\" : 1");
        }

        @Test
        @DisplayName("should omit zero-count severities")
        void shouldOmitZeroCountSeverities() {
            // Given
            var counts = new ViolationCounts(10, 0, 0, 10, 0, 0);

            // When
            String diagram = builder.build(counts);

            // Then
            assertThat(diagram).contains("\"MAJOR\" : 10");
            assertThat(diagram).doesNotContain("\"BLOCKER\"");
            assertThat(diagram).doesNotContain("\"CRITICAL\"");
            assertThat(diagram).doesNotContain("\"MINOR\"");
            assertThat(diagram).doesNotContain("\"INFO\"");
        }

        @Test
        @DisplayName("should handle empty violations")
        void shouldHandleEmptyViolations() {
            // Given
            var counts = ViolationCounts.empty();

            // When
            String diagram = builder.build(counts);

            // Then
            assertThat(diagram).contains("\"No violations\" : 1");
        }
    }

    @Nested
    @DisplayName("QuadrantChartBuilder")
    class QuadrantChartBuilderTests {

        private final QuadrantChartBuilder builder = new QuadrantChartBuilder();

        @Test
        @DisplayName("should generate quadrant chart with packages")
        void shouldGenerateQuadrantChartWithPackages() {
            // Given
            var packages = List.of(
                    new PackageMetric(
                            "com.example.domain.order", 2, 1, 0.33, 0.0, 0.67, PackageMetric.ZoneType.STABLE_CORE),
                    new PackageMetric(
                            "com.example.port.driven", 0, 3, 1.0, 1.0, 0.0, PackageMetric.ZoneType.MAIN_SEQUENCE));

            // When
            String diagram = builder.build(packages);

            // Then
            assertThat(diagram).contains("quadrantChart");
            assertThat(diagram).contains("title Package Stability Analysis");
            assertThat(diagram).contains("x-axis Concrete --> Abstract");
            assertThat(diagram).contains("y-axis Stable --> Unstable");
            assertThat(diagram).contains("quadrant-1 Zone of Uselessness");
            assertThat(diagram).contains("quadrant-2 Main Sequence");
            assertThat(diagram).contains("domain.order");
            assertThat(diagram).contains("port.driven");
        }

        @Test
        @DisplayName("should handle empty package list")
        void shouldHandleEmptyPackageList() {
            // When
            String diagram = builder.build(List.of());

            // Then
            assertThat(diagram).contains("quadrantChart");
            assertThat(diagram).contains("\"No packages\"");
        }
    }

    @Nested
    @DisplayName("ClassDiagramBuilder")
    class ClassDiagramBuilderTests {

        private final ClassDiagramBuilder builder = new ClassDiagramBuilder();

        @Test
        @DisplayName("should generate class diagram with aggregates and VOs")
        void shouldGenerateClassDiagramWithAggregatesAndVOs() {
            // Given
            var aggregates = List.of(
                    new AggregateComponent(
                            "Order",
                            "com.example.domain.order",
                            9,
                            List.of("InventoryItem"),
                            List.of("OrderRepository")));
            var valueObjects = List.of(
                    new ValueObjectComponent("Money", "com.example.domain.order"),
                    new ValueObjectComponent("OrderLine", "com.example.domain.order"));
            var identifiers = List.of(new IdentifierComponent("OrderId", "com.example.domain.order", "java.util.UUID"));
            var components =
                    new ComponentDetails(aggregates, valueObjects, identifiers, List.of(), List.of(), List.of());
            List<Relationship> relationships = List.of();

            // When
            String diagram = builder.build(components, relationships);

            // Then
            assertThat(diagram).contains("classDiagram");
            assertThat(diagram).contains("class Order~AggregateRoot~");
            assertThat(diagram).contains("class Money~ValueObject~");
            assertThat(diagram).contains("class OrderId~Identifier~");
            assertThat(diagram).contains("Order *-- Money");
            assertThat(diagram).contains("Order *-- OrderId");
        }

        @Test
        @DisplayName("should show cycles in relationships")
        void shouldShowCyclesInRelationships() {
            // Given - need aggregates to have relationships displayed
            var aggregates = List.of(
                    new AggregateComponent("Order", "com.example.order", 5, List.of("InventoryItem"), List.of()),
                    new AggregateComponent("InventoryItem", "com.example.inventory", 3, List.of("Order"), List.of()));
            var components = new ComponentDetails(aggregates, List.of(), List.of(), List.of(), List.of(), List.of());
            var relationships = List.of(Relationship.cycle("Order", "InventoryItem", "references"));

            // When
            String diagram = builder.build(components, relationships);

            // Then
            assertThat(diagram).contains("Order <..> InventoryItem : CYCLE");
        }
    }

    @Nested
    @DisplayName("C4ContextDiagramBuilder")
    class C4ContextDiagramBuilderTests {

        private final C4ContextDiagramBuilder builder = new C4ContextDiagramBuilder();

        @Test
        @DisplayName("should generate C4Context diagram")
        void shouldGenerateC4ContextDiagram() {
            // Given
            var drivingPorts = List.of(PortComponent.driving(
                    "OrderUseCase", "com.example.port.driving", 8, false, null, List.of("Order")));
            var drivenPorts = List.of(
                    PortComponent.driven("OrderRepository", "com.example.port.driven", "REPOSITORY", 6, false, null),
                    PortComponent.driven("PaymentGateway", "com.example.port.driven", "GATEWAY", 3, false, null));

            // When
            String diagram = builder.build("E-Commerce", drivingPorts, drivenPorts);

            // Then
            assertThat(diagram).contains("C4Context");
            assertThat(diagram).contains("title System Context - E-Commerce");
            assertThat(diagram).contains("Person(user");
            assertThat(diagram).contains("System(app");
            assertThat(diagram).contains("System_Ext(");
            assertThat(diagram).contains("Database");
            assertThat(diagram).contains("Payment Provider");
        }
    }

    @Nested
    @DisplayName("C4ComponentDiagramBuilder")
    class C4ComponentDiagramBuilderTests {

        private final C4ComponentDiagramBuilder builder = new C4ComponentDiagramBuilder();

        @Test
        @DisplayName("should generate C4Component diagram")
        void shouldGenerateC4ComponentDiagram() {
            // Given
            var aggregates = List.of(
                    new AggregateComponent("Order", "com.example.domain", 9, List.of(), List.of("OrderRepository")));
            var drivingPorts = List.of(
                    PortComponent.driving("OrderUseCase", "com.example.port", 8, false, null, List.of("Order")));
            var drivenPorts = List.of(
                    PortComponent.driven("OrderRepository", "com.example.port", "REPOSITORY", 6, true, "JpaOrderRepo"));
            var adapters =
                    List.of(new AdapterComponent("JpaOrderRepo", "com.example.infra", "OrderRepository", AdapterComponent.AdapterType.DRIVEN));
            var components = new ComponentDetails(aggregates, List.of(), List.of(), drivingPorts, drivenPorts, adapters);

            // When
            String diagram = builder.build("E-Commerce", components, List.of());

            // Then
            assertThat(diagram).contains("C4Component");
            assertThat(diagram).contains("title Component Diagram - E-Commerce");
            assertThat(diagram).contains("Container_Boundary(driving");
            assertThat(diagram).contains("Container_Boundary(domain");
            assertThat(diagram).contains("Container_Boundary(driven");
            assertThat(diagram).contains("Component(orderusecase");
            assertThat(diagram).contains("Component(order");
            assertThat(diagram).contains("Rel_D(");
        }

        @Test
        @DisplayName("should highlight cycles")
        void shouldHighlightCycles() {
            // Given
            var components = new ComponentDetails(
                    List.of(
                            new AggregateComponent("Order", "pkg", 5, List.of(), List.of()),
                            new AggregateComponent("Inventory", "pkg", 3, List.of(), List.of())),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());
            var relationships = List.of(Relationship.cycle("Order", "Inventory", "references"));

            // When
            String diagram = builder.build("Test", components, relationships);

            // Then
            assertThat(diagram).contains("BiRel(order, inventory, \"cycle!\")");
            assertThat(diagram).contains("UpdateRelStyle(order, inventory, $lineColor=\"red\"");
        }
    }

    @Nested
    @DisplayName("DiagramGenerator")
    class DiagramGeneratorTests {

        @Test
        @DisplayName("should generate all 6 diagrams")
        void shouldGenerateAllSixDiagrams() {
            // Given
            var generator = new DiagramGenerator();
            var reportData = createTestReportData();

            // When
            DiagramSet diagrams = generator.generate(reportData, "Test Project");

            // Then
            assertThat(diagrams.scoreRadar()).isNotBlank().contains("radar-beta");
            assertThat(diagrams.c4Context()).isNotBlank().contains("C4Context");
            assertThat(diagrams.c4Component()).isNotBlank().contains("C4Component");
            assertThat(diagrams.domainModel()).isNotBlank().contains("classDiagram");
            assertThat(diagrams.violationsPie()).isNotBlank().contains("pie");
            assertThat(diagrams.packageZones()).isNotBlank().contains("quadrantChart");
        }
    }

    // Helper methods

    private ScoreBreakdown createScoreBreakdown(int ddd, int hex, int dep, int cpl, int coh) {
        return new ScoreBreakdown(
                ScoreDimension.of(25, ddd),
                ScoreDimension.of(25, hex),
                ScoreDimension.of(20, dep),
                ScoreDimension.of(15, cpl),
                ScoreDimension.of(15, coh));
    }

    private ReportData createTestReportData() {
        var metadata = new ReportMetadata(
                "Test", "1.0", java.time.Instant.now(), "10ms", "2.0.0", "2.0.0");

        var verdict = new Verdict(
                73, "C", ReportStatus.FAILED, "Test", "Summary",
                List.of(), ImmediateAction.none());

        var totals = new InventoryTotals(1, 0, 2, 1, 0, 1, 1);
        var inventory = new Inventory(List.of(new BoundedContextInventory("Test", 1, 0, 2, 0)), totals);
        var components = new ComponentDetails(
                List.of(new AggregateComponent("Order", "pkg", 5, List.of(), List.of())),
                List.of(new ValueObjectComponent("Money", "pkg")),
                List.of(new IdentifierComponent("OrderId", "pkg", "UUID")),
                List.of(PortComponent.driving("OrderUseCase", "pkg", 3, false, null, List.of("Order"))),
                List.of(PortComponent.driven("OrderRepo", "pkg", "REPOSITORY", 4, false, null)),
                List.of());
        var architecture = new ArchitectureOverview(
                "Test arch", inventory, components, DiagramsInfo.defaults(), List.of());

        var issues = IssuesSummary.empty();
        var remediation = RemediationPlan.empty();
        var appendix = new Appendix(
                createScoreBreakdown(78, 60, 100, 24, 100),
                List.of(),
                List.of(),
                List.of(new PackageMetric("pkg", 1, 1, 0.5, 0.0, 0.5, PackageMetric.ZoneType.STABLE_CORE)));

        return ReportData.create(metadata, verdict, architecture, issues, remediation, appendix);
    }
}
