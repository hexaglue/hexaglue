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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory.BoundedContextStats;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for C4DiagramBuilder.
 *
 * <p>Tests use TestModelBuilder to create ArchitecturalModel fixtures for
 * verifying diagram generation.
 *
 * @since 5.0.0 Implemented using TestModelBuilder fixtures
 */
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
            List<BoundedContextStats> contexts = List.of(new BoundedContextStats("Order", 2, 3, 5, 4, 1000));

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
            // Given: Model with driving and driven ports
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderUseCase")
                    .addDrivenPort("com.example.port.OrderRepository", DrivenPortType.REPOSITORY)
                    .build();

            // When
            String diagram = builder.buildContainerDiagram("TestApp", model, null);

            // Then
            assertThat(diagram).startsWith("```mermaid");
            assertThat(diagram).endsWith("```\n");
            assertThat(diagram).contains("flowchart LR");
            assertThat(diagram).contains("OrderUseCase");
            assertThat(diagram).contains("OrderRepository");
        }

        @Test
        @DisplayName("should separate driving and driven ports")
        void shouldSeparateDrivingAndDrivenPorts() {
            // Given: Model with both port types
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.CreateOrderUseCase")
                    .addDrivingPort("com.example.port.CancelOrderUseCase")
                    .addDrivenPort("com.example.port.OrderRepository", DrivenPortType.REPOSITORY)
                    .addDrivenPort("com.example.port.PaymentGateway", DrivenPortType.GATEWAY)
                    .build();

            // When
            String diagram = builder.buildContainerDiagram("TestApp", model, null);

            // Then: Verify driving ports subgraph
            assertThat(diagram).contains("subgraph DRIVING");
            assertThat(diagram).contains("Driving Adapters");
            assertThat(diagram).contains("CreateOrderUseCase");
            assertThat(diagram).contains("CancelOrderUseCase");

            // Then: Verify driven ports subgraph
            assertThat(diagram).contains("subgraph DRIVEN");
            assertThat(diagram).contains("Driven Adapters");
            assertThat(diagram).contains("OrderRepository");
            assertThat(diagram).contains("PaymentGateway");
        }
    }

    @Nested
    @DisplayName("Aggregate Diagram")
    class AggregateDiagramTests {

        @Test
        @DisplayName("should generate aggregate diagram with relationships")
        void shouldGenerateAggregateDiagram() {
            // Given: Model with aggregates
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.order.Order")
                    .addAggregateRoot("com.example.customer.Customer")
                    .build();

            // When
            String diagram = builder.buildAggregateDiagram(model);

            // Then
            assertThat(diagram).startsWith("```mermaid");
            assertThat(diagram).endsWith("```\n");
            assertThat(diagram).contains("flowchart TB");
            assertThat(diagram).contains("Aggregate Root");
            assertThat(diagram).contains("Order");
            assertThat(diagram).contains("Customer");
        }

        @Test
        @DisplayName("should handle no aggregates")
        void shouldHandleNoAggregates() {
            // Given: Empty model with no aggregates
            ArchitecturalModel model = TestModelBuilder.emptyModel();

            // When
            String diagram = builder.buildAggregateDiagram(model);

            // Then
            assertThat(diagram).contains("No Aggregate Roots detected");
        }

        @Test
        @DisplayName("should group entities with their aggregate")
        void shouldGroupEntitiesWithAggregate() {
            // Given: Model with aggregate, entities and value objects in same package
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.order.Order")
                    .addEntity("com.example.order.OrderLine", "com.example.order.Order")
                    .addValueObject("com.example.order.Money")
                    .build();

            // When
            String diagram = builder.buildAggregateDiagram(model);

            // Then: Aggregate root and children should be in the diagram
            assertThat(diagram).contains("Order");
            assertThat(diagram).contains("OrderLine");
            assertThat(diagram).contains("Money");
            // Entities are shown with square brackets, VOs with parentheses
            assertThat(diagram).contains("[OrderLine]");
            assertThat(diagram).contains("(Money)");
        }
    }

    @Nested
    @DisplayName("Port Matrix Diagram")
    class PortMatrixDiagramTests {

        @Test
        @DisplayName("should generate port matrix with icons")
        void shouldGeneratePortMatrixWithIcons() {
            // Given: Model with various ports
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderUseCase")
                    .addDrivenPort("com.example.port.OrderRepository", DrivenPortType.REPOSITORY)
                    .addDrivenPort("com.example.port.NotificationService", DrivenPortType.GATEWAY)
                    .build();

            // When
            String diagram = builder.buildPortMatrixDiagram(model);

            // Then
            assertThat(diagram).startsWith("```mermaid");
            assertThat(diagram).endsWith("```\n");
            assertThat(diagram).contains("flowchart LR");
            assertThat(diagram).contains("DRIVING PORTS");
            assertThat(diagram).contains("DRIVEN PORTS");
            // All ports get icons
            assertThat(diagram).contains("🔌");
        }

        @Test
        @DisplayName("should show repository icon for repositories")
        void shouldShowRepositoryIcon() {
            // Given: Model with repository port
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivenPort("com.example.port.OrderRepository", DrivenPortType.REPOSITORY)
                    .addDrivenPort("com.example.port.PaymentGateway", DrivenPortType.GATEWAY)
                    .build();

            // When
            String diagram = builder.buildPortMatrixDiagram(model);

            // Then: Repository should have 📦 icon
            assertThat(diagram).contains("📦 OrderRepository");
            // Gateway should have 🔌 icon
            assertThat(diagram).contains("🔌 PaymentGateway");
        }

        @Test
        @DisplayName("should handle empty ports")
        void shouldHandleEmptyPorts() {
            // Given: Empty model with no ports
            ArchitecturalModel model = TestModelBuilder.emptyModel();

            // When
            String diagram = builder.buildPortMatrixDiagram(model);

            // Then
            assertThat(diagram).contains("No driving ports");
            assertThat(diagram).contains("No driven ports");
        }
    }
}
