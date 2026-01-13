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

import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory.BoundedContextStats;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.testing.DomainTypeBuilder;
import io.hexaglue.spi.ir.testing.IrSnapshotBuilder;
import io.hexaglue.spi.ir.testing.PortBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("C4DiagramBuilder")
class C4DiagramBuilderTest {

    private C4DiagramBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new C4DiagramBuilder();
    }

    @Nested
    @DisplayName("System Context Diagram")
    class SystemContextDiagramTests {

        @Test
        @DisplayName("should generate valid Mermaid syntax")
        void shouldGenerateValidMermaidSyntax() {
            List<BoundedContextStats> contexts = List.of(
                    new BoundedContextStats("Order", 2, 3, 5, 4, 1000),
                    new BoundedContextStats("Customer", 1, 2, 3, 2, 500));

            String diagram = builder.buildSystemContextDiagram("E-Commerce", contexts);

            assertThat(diagram).startsWith("```mermaid");
            assertThat(diagram).endsWith("```\n");
            assertThat(diagram).contains("flowchart TB");
        }

        @Test
        @DisplayName("should include bounded contexts")
        void shouldIncludeBoundedContexts() {
            List<BoundedContextStats> contexts = List.of(
                    new BoundedContextStats("Order", 2, 3, 5, 4, 1000));

            String diagram = builder.buildSystemContextDiagram("MyApp", contexts);

            assertThat(diagram).contains("Order");
            assertThat(diagram).contains("Bounded Context");
            assertThat(diagram).contains("2 Aggregates, 3 Entities");
        }

        @Test
        @DisplayName("should handle empty bounded contexts")
        void shouldHandleEmptyBoundedContexts() {
            String diagram = builder.buildSystemContextDiagram("MyApp", List.of());

            assertThat(diagram).contains("No bounded contexts detected");
        }

        @Test
        @DisplayName("should include external actors")
        void shouldIncludeExternalActors() {
            String diagram = builder.buildSystemContextDiagram("MyApp", List.of());

            assertThat(diagram).contains("User");
            assertThat(diagram).contains("External Systems");
        }
    }

    @Nested
    @DisplayName("Container Diagram")
    class ContainerDiagramTests {

        @Test
        @DisplayName("should generate container diagram with ports")
        void shouldGenerateContainerDiagramWithPorts() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.test")
                    .withPort(PortBuilder.useCase("com.test.OrderService").build())
                    .withPort(PortBuilder.repository("com.test.OrderRepository").build())
                    .build();

            String diagram = builder.buildContainerDiagram("MyApp", ir, null);

            assertThat(diagram).startsWith("```mermaid");
            assertThat(diagram).contains("flowchart LR");
            assertThat(diagram).contains("Driving Adapters");
            assertThat(diagram).contains("Driven Adapters");
            assertThat(diagram).contains("Domain Core");
        }

        @Test
        @DisplayName("should separate driving and driven ports")
        void shouldSeparateDrivingAndDrivenPorts() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.test")
                    .withPort(PortBuilder.useCase("com.test.OrderService").build())
                    .withPort(PortBuilder.repository("com.test.OrderRepository").build())
                    .build();

            String diagram = builder.buildContainerDiagram("MyApp", ir, null);

            assertThat(diagram).contains("DRIVING");
            assertThat(diagram).contains("DRIVEN");
        }
    }

    @Nested
    @DisplayName("Aggregate Diagram")
    class AggregateDiagramTests {

        @Test
        @DisplayName("should generate aggregate diagram with relationships")
        void shouldGenerateAggregateDiagram() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.test")
                    .withDomainType(DomainTypeBuilder.aggregateRoot("com.test.order.Order").build())
                    .build();

            String diagram = builder.buildAggregateDiagram(ir);

            assertThat(diagram).startsWith("```mermaid");
            assertThat(diagram).contains("flowchart TB");
            assertThat(diagram).contains("Order");
            assertThat(diagram).contains("Aggregate Root");
        }

        @Test
        @DisplayName("should handle no aggregates")
        void shouldHandleNoAggregates() {
            IrSnapshot ir = IrSnapshot.empty("com.test");

            String diagram = builder.buildAggregateDiagram(ir);

            assertThat(diagram).contains("No Aggregate Roots detected");
        }

        @Test
        @DisplayName("should group entities with their aggregate")
        void shouldGroupEntitiesWithAggregate() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.test")
                    .withDomainType(DomainTypeBuilder.aggregateRoot("com.test.order.Order").build())
                    .withDomainType(DomainTypeBuilder.entity("com.test.order.OrderLine").build())
                    .build();

            String diagram = builder.buildAggregateDiagram(ir);

            assertThat(diagram).contains("Order");
            assertThat(diagram).contains("OrderLine");
        }
    }

    @Nested
    @DisplayName("Port Matrix Diagram")
    class PortMatrixDiagramTests {

        @Test
        @DisplayName("should generate port matrix with icons")
        void shouldGeneratePortMatrixWithIcons() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.test")
                    .withPort(PortBuilder.useCase("com.test.OrderService").build())
                    .withPort(PortBuilder.repository("com.test.OrderRepository").build())
                    .build();

            String diagram = builder.buildPortMatrixDiagram(ir);

            assertThat(diagram).contains("DRIVING PORTS");
            assertThat(diagram).contains("DRIVEN PORTS");
            assertThat(diagram).contains("Domain Core");
        }

        @Test
        @DisplayName("should show repository icon for repositories")
        void shouldShowRepositoryIcon() {
            IrSnapshot ir = IrSnapshotBuilder.create("com.test")
                    .withPort(PortBuilder.repository("com.test.OrderRepository").build())
                    .build();

            String diagram = builder.buildPortMatrixDiagram(ir);

            assertThat(diagram).contains("📦");
        }

        @Test
        @DisplayName("should handle empty ports")
        void shouldHandleEmptyPorts() {
            IrSnapshot ir = IrSnapshot.empty("com.test");

            String diagram = builder.buildPortMatrixDiagram(ir);

            assertThat(diagram).contains("No driving ports");
            assertThat(diagram).contains("No driven ports");
        }
    }
}
