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

package io.hexaglue.plugin.audit.adapter.report;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import io.hexaglue.plugin.audit.adapter.report.model.AuditSummary;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory.BoundedContextStats;
import io.hexaglue.plugin.audit.adapter.report.model.ConstraintsSummary;
import io.hexaglue.plugin.audit.adapter.report.model.HealthScore;
import io.hexaglue.plugin.audit.adapter.report.model.ReportMetadata;
import io.hexaglue.spi.audit.DetectedArchitectureStyle;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.testing.DomainTypeBuilder;
import io.hexaglue.spi.ir.testing.IrSnapshotBuilder;
import io.hexaglue.spi.ir.testing.PortBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("DocumentationGenerator")
class DocumentationGeneratorTest {

    private DocumentationGenerator generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        generator = new DocumentationGenerator();
    }

    private AuditReport createMinimalReport() {
        ReportMetadata metadata = new ReportMetadata(
                "TestProject", "1.0.0", Instant.now(), "100ms", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        ConstraintsSummary constraints = new ConstraintsSummary(0, List.of());

        return new AuditReport(metadata, summary, List.of(), List.of(), constraints);
    }

    private AuditReport createReportWithIr() {
        ReportMetadata metadata = new ReportMetadata(
                "TestProject", "1.0.0", Instant.now(), "100ms", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        ConstraintsSummary constraints = new ConstraintsSummary(0, List.of());

        IrSnapshot ir = IrSnapshotBuilder.create("com.test")
                .withDomainType(DomainTypeBuilder.aggregateRoot("com.test.order.Order").build())
                .withDomainType(DomainTypeBuilder.entity("com.test.order.OrderLine").build())
                .withDomainType(DomainTypeBuilder.valueObject("com.test.order.OrderId").build())
                .withPort(PortBuilder.useCase("com.test.OrderService").build())
                .withPort(PortBuilder.repository("com.test.OrderRepository").build())
                .build();

        // ComponentInventory(aggregateRoots, entities, valueObjects, domainEvents, domainServices,
        //   applicationServices, drivingPorts, drivenPorts, totalDomainTypes, totalPorts,
        //   aggregateExamples, entityExamples, valueObjectExamples, domainEventExamples,
        //   domainServiceExamples, drivingPortExamples, drivenPortExamples, boundedContexts)
        ComponentInventory inventory = new ComponentInventory(
                1, 1, 1, 0, 0,  // aggregates, entities, valueObjects, domainEvents, domainServices
                0, 1, 1, 3, 2,  // applicationServices, drivingPorts, drivenPorts, totalDomainTypes, totalPorts
                List.of("Order"), List.of("OrderLine"), List.of("OrderId"),
                List.of(), List.of(), List.of("OrderService"), List.of("OrderRepository"),
                List.of(new BoundedContextStats("order", 1, 1, 1, 2, 500)));

        // HealthScore(overall, dddCompliance, hexCompliance, dependencyQuality, coupling, cohesion, grade)
        HealthScore healthScore = new HealthScore(85, 90, 80, 85, 80, 85, "B");

        return new AuditReport(
                metadata, summary, List.of(), List.of(), constraints,
                null, healthScore, inventory, List.of(), null, List.of(), null,
                90, 80, DetectedArchitectureStyle.HEXAGONAL, List.of(),
                ir, null);
    }

    @Nested
    @DisplayName("generateAll")
    class GenerateAllTests {

        @Test
        @DisplayName("should create all documentation files")
        void shouldCreateAllDocumentationFiles() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateAll(report, tempDir);

            assertThat(tempDir.resolve("ARCHITECTURE-OVERVIEW.md")).exists();
            assertThat(tempDir.resolve("DOMAIN-MODEL.md")).exists();
            assertThat(tempDir.resolve("PORTS-AND-ADAPTERS.md")).exists();
            assertThat(tempDir.resolve("BOUNDED-CONTEXTS.md")).exists();
            assertThat(tempDir.resolve("METRICS.md")).exists();
            assertThat(tempDir.resolve("diagrams")).isDirectory();
        }

        @Test
        @DisplayName("should create diagrams directory with Mermaid files")
        void shouldCreateDiagramsDirectory() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateAll(report, tempDir);

            Path diagramsDir = tempDir.resolve("diagrams");
            assertThat(diagramsDir.resolve("c4-system-context.md")).exists();
            assertThat(diagramsDir.resolve("c4-container.md")).exists();
            assertThat(diagramsDir.resolve("aggregate-graph.md")).exists();
            assertThat(diagramsDir.resolve("port-matrix.md")).exists();
        }
    }

    @Nested
    @DisplayName("generateArchitectureOverview")
    class ArchitectureOverviewTests {

        @Test
        @DisplayName("should generate valid Markdown with project name")
        void shouldGenerateValidMarkdown() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateArchitectureOverview(report, tempDir);

            String content = Files.readString(tempDir.resolve("ARCHITECTURE-OVERVIEW.md"));
            assertThat(content).contains("# Architecture Overview: TestProject");
            assertThat(content).contains("## Executive Summary");
            assertThat(content).contains("## Component Inventory");
        }

        @Test
        @DisplayName("should include health score")
        void shouldIncludeHealthScore() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateArchitectureOverview(report, tempDir);

            String content = Files.readString(tempDir.resolve("ARCHITECTURE-OVERVIEW.md"));
            assertThat(content).contains("Health Score");
            assertThat(content).contains("85/100");
        }

        @Test
        @DisplayName("should include C4 diagrams when IR is available")
        void shouldIncludeC4Diagrams() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateArchitectureOverview(report, tempDir);

            String content = Files.readString(tempDir.resolve("ARCHITECTURE-OVERVIEW.md"));
            assertThat(content).contains("## System Context (C4 Level 1)");
            assertThat(content).contains("## Container View (C4 Level 2)");
            assertThat(content).contains("```mermaid");
        }

        @Test
        @DisplayName("should include links to other documents")
        void shouldIncludeDocumentLinks() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateArchitectureOverview(report, tempDir);

            String content = Files.readString(tempDir.resolve("ARCHITECTURE-OVERVIEW.md"));
            assertThat(content).contains("[Domain Model](DOMAIN-MODEL.md)");
            assertThat(content).contains("[Ports & Adapters](PORTS-AND-ADAPTERS.md)");
            assertThat(content).contains("[Bounded Contexts](BOUNDED-CONTEXTS.md)");
            assertThat(content).contains("[Metrics](METRICS.md)");
        }
    }

    @Nested
    @DisplayName("generateDomainModel")
    class DomainModelTests {

        @Test
        @DisplayName("should generate aggregate graph")
        void shouldGenerateAggregateGraph() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateDomainModel(report, tempDir);

            String content = Files.readString(tempDir.resolve("DOMAIN-MODEL.md"));
            assertThat(content).contains("# Domain Model: TestProject");
            assertThat(content).contains("## Aggregate Relationship Graph");
            assertThat(content).contains("```mermaid");
        }

        @Test
        @DisplayName("should list entities by type")
        void shouldListEntitiesByType() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateDomainModel(report, tempDir);

            String content = Files.readString(tempDir.resolve("DOMAIN-MODEL.md"));
            assertThat(content).contains("## Entities");
            assertThat(content).contains("OrderLine");
        }

        @Test
        @DisplayName("should list value objects")
        void shouldListValueObjects() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateDomainModel(report, tempDir);

            String content = Files.readString(tempDir.resolve("DOMAIN-MODEL.md"));
            assertThat(content).contains("## Value Objects");
            assertThat(content).contains("OrderId");
        }
    }

    @Nested
    @DisplayName("generatePortsAndAdapters")
    class PortsAndAdaptersTests {

        @Test
        @DisplayName("should generate port matrix diagram")
        void shouldGeneratePortMatrixDiagram() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generatePortsAndAdapters(report, tempDir);

            String content = Files.readString(tempDir.resolve("PORTS-AND-ADAPTERS.md"));
            assertThat(content).contains("# Ports & Adapters: TestProject");
            assertThat(content).contains("## Port Matrix Diagram");
            assertThat(content).contains("```mermaid");
        }

        @Test
        @DisplayName("should list driving ports")
        void shouldListDrivingPorts() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generatePortsAndAdapters(report, tempDir);

            String content = Files.readString(tempDir.resolve("PORTS-AND-ADAPTERS.md"));
            assertThat(content).contains("## Driving Ports");
            assertThat(content).contains("OrderService");
        }

        @Test
        @DisplayName("should list driven ports")
        void shouldListDrivenPorts() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generatePortsAndAdapters(report, tempDir);

            String content = Files.readString(tempDir.resolve("PORTS-AND-ADAPTERS.md"));
            assertThat(content).contains("## Driven Ports");
            assertThat(content).contains("OrderRepository");
        }

        @Test
        @DisplayName("should include summary table")
        void shouldIncludeSummaryTable() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generatePortsAndAdapters(report, tempDir);

            String content = Files.readString(tempDir.resolve("PORTS-AND-ADAPTERS.md"));
            assertThat(content).contains("## Summary");
            assertThat(content).contains("Driving Ports");
            assertThat(content).contains("Driven Ports");
        }
    }

    @Nested
    @DisplayName("generateBoundedContexts")
    class BoundedContextsTests {

        @Test
        @DisplayName("should generate context breakdown table")
        void shouldGenerateContextBreakdown() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateBoundedContexts(report, tempDir);

            String content = Files.readString(tempDir.resolve("BOUNDED-CONTEXTS.md"));
            assertThat(content).contains("# Bounded Contexts: TestProject");
            assertThat(content).contains("## Context Breakdown");
            assertThat(content).contains("order");
        }

        @Test
        @DisplayName("should handle empty bounded contexts")
        void shouldHandleEmptyBoundedContexts() throws IOException {
            AuditReport report = createMinimalReport();

            generator.generateBoundedContexts(report, tempDir);

            String content = Files.readString(tempDir.resolve("BOUNDED-CONTEXTS.md"));
            assertThat(content).contains("## No Bounded Contexts Detected");
        }

        @Test
        @DisplayName("should include individual context details")
        void shouldIncludeContextDetails() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateBoundedContexts(report, tempDir);

            String content = Files.readString(tempDir.resolve("BOUNDED-CONTEXTS.md"));
            assertThat(content).contains("## Context Details");
            assertThat(content).contains("### order");
        }
    }

    @Nested
    @DisplayName("generateMetrics")
    class MetricsTests {

        @Test
        @DisplayName("should include health score breakdown")
        void shouldIncludeHealthScoreBreakdown() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateMetrics(report, tempDir);

            String content = Files.readString(tempDir.resolve("METRICS.md"));
            assertThat(content).contains("# Architecture Metrics: TestProject");
            assertThat(content).contains("## Health Score: 85/100");
            assertThat(content).contains("DDD Compliance");
            assertThat(content).contains("Hexagonal Compliance");
        }
    }

    @Nested
    @DisplayName("generateDiagramFiles")
    class DiagramFilesTests {

        @Test
        @DisplayName("should generate all Mermaid diagram files")
        void shouldGenerateAllDiagramFiles() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateDiagramFiles(report, tempDir);

            Path diagrams = tempDir.resolve("diagrams");
            assertThat(diagrams.resolve("c4-system-context.md")).exists();
            assertThat(diagrams.resolve("c4-container.md")).exists();
            assertThat(diagrams.resolve("aggregate-graph.md")).exists();
            assertThat(diagrams.resolve("port-matrix.md")).exists();
        }

        @Test
        @DisplayName("diagram files should contain valid Mermaid code")
        void diagramFilesShouldContainMermaid() throws IOException {
            AuditReport report = createReportWithIr();

            generator.generateDiagramFiles(report, tempDir);

            String content = Files.readString(tempDir.resolve("diagrams/c4-system-context.md"));
            assertThat(content).startsWith("```mermaid");
            assertThat(content).endsWith("```\n");
        }
    }
}
