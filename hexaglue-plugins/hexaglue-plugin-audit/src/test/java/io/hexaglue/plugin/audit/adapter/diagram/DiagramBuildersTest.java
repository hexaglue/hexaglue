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
            var aggregates = List.of(AggregateComponent.of(
                    "Order", "com.example.domain.order", 9, List.of("InventoryItem"), List.of("OrderRepository")));
            var valueObjects = List.of(
                    ValueObjectComponent.of("Money", "com.example.domain.order"),
                    ValueObjectComponent.of("OrderLine", "com.example.domain.order"));
            var identifiers = List.of(new IdentifierComponent("OrderId", "com.example.domain.order", "java.util.UUID"));
            var components =
                    ComponentDetails.of(aggregates, valueObjects, identifiers, List.of(), List.of(), List.of());
            // Composition relationships are now explicitly provided (cross-package support)
            List<Relationship> relationships = List.of(
                    Relationship.of("Order", "Money", "contains"),
                    Relationship.of("Order", "OrderLine", "contains"),
                    Relationship.of("Order", "OrderId", "owns"));

            // When
            String diagram = builder.build(components, relationships);

            // Then
            assertThat(diagram).contains("title: Domain Model");
            assertThat(diagram).contains("classDiagram");
            assertThat(diagram).contains("class Order{");
            assertThat(diagram).contains("<<AggregateRoot>>");
            assertThat(diagram).contains("class Money{");
            assertThat(diagram).contains("<<ValueObject>>");
            assertThat(diagram).contains("class OrderId{");
            assertThat(diagram).contains("<<Identifier>>");
            assertThat(diagram).contains("Order *-- Money");
            assertThat(diagram).contains("Order *-- OrderId");
        }

        @Test
        @DisplayName("should show cycles in relationships with Alert styling")
        void shouldShowCyclesInRelationships() {
            // Given - need aggregates to have relationships displayed
            var aggregates = List.of(
                    AggregateComponent.of("Order", "com.example.order", 5, List.of("InventoryItem"), List.of()),
                    AggregateComponent.of("InventoryItem", "com.example.inventory", 3, List.of("Order"), List.of()));
            var components = ComponentDetails.of(aggregates, List.of(), List.of(), List.of(), List.of(), List.of());
            var relationships = List.of(Relationship.cycle("Order", "InventoryItem", "references"));

            // When
            String diagram = builder.build(components, relationships);

            // Then - cycle with exclamation marks
            assertThat(diagram).contains("Order <..> InventoryItem : CYCLE!!");
            // Alert styling for cycle participants
            assertThat(diagram).contains("class Order:::Alert");
            assertThat(diagram).contains("class InventoryItem:::Alert");
            assertThat(diagram).contains("classDef Alert stroke:#FF5978,fill:#FFDFE5,color:#8E2236,stroke-width:2px");
        }

        @Test
        @DisplayName("should show mutable value object with MutableWarning styling")
        void shouldShowMutableValueObjectWithWarningStyle() {
            // Given
            var aggregates = List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of()));
            var valueObjects = List.of(ValueObjectComponent.of("Money", "com.example.order"));
            var components = ComponentDetails.of(aggregates, valueObjects, List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.mutableValueObject("Money"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then - MutableWarning styling for mutable VO
            assertThat(diagram).contains("class Money:::MutableWarning");
            assertThat(diagram)
                    .contains("classDef MutableWarning stroke:#FF9800,fill:#FFF3E0,color:#E65100,stroke-width:2px");
        }

        @Test
        @DisplayName("should show impure domain type with ImpurityWarning styling")
        void shouldShowImpureDomainTypeWithWarningStyle() {
            // Given
            var aggregates = List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of()));
            var valueObjects = List.of(ValueObjectComponent.of("JpaEntity", "com.example.order"));
            var components = ComponentDetails.of(aggregates, valueObjects, List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.impureDomain("JpaEntity"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then - ImpurityWarning styling for impure type
            assertThat(diagram).contains("class JpaEntity:::ImpurityWarning");
            assertThat(diagram)
                    .contains("classDef ImpurityWarning stroke:#9C27B0,fill:#F3E5F5,color:#6A1B9A,stroke-width:2px");
        }

        @Test
        @DisplayName("should prioritize cycle style over other violations")
        void shouldPrioritizeCycleStyleOverOthers() {
            // Given - Order has both cycle and mutable violation
            var aggregates = List.of(
                    AggregateComponent.of("Order", "com.example.order", 5, List.of("Inventory"), List.of()),
                    AggregateComponent.of("Inventory", "com.example.inventory", 3, List.of("Order"), List.of()));
            var components = ComponentDetails.of(aggregates, List.of(), List.of(), List.of(), List.of(), List.of());
            var relationships = List.of(Relationship.cycle("Order", "Inventory", "references"));
            var typeViolations = List.of(
                    TypeViolation.mutableValueObject("Order") // This should be ignored for Order
                    );

            // When
            String diagram = builder.build(components, relationships, typeViolations);

            // Then - Alert takes priority, Order should only have Alert style
            assertThat(diagram).contains("class Order:::Alert");
            assertThat(diagram).doesNotContain("class Order:::MutableWarning");
        }

        // --- New tests for extended violation types (Phase 2) ---

        @Test
        @DisplayName("should show missing identity with MissingIdentityWarning styling")
        void shouldShowMissingIdentityWithWarningStyle() {
            // Given
            var aggregates = List.of(AggregateComponent.of("Customer", "com.example.domain", 3, List.of(), List.of()));
            var components = ComponentDetails.of(aggregates, List.of(), List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.missingIdentity("Customer"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then
            assertThat(diagram).contains("class Customer:::MissingIdentityWarning");
            assertThat(diagram)
                    .contains(
                            "classDef MissingIdentityWarning stroke:#FBC02D,fill:#FFFDE7,color:#F57F17,stroke-width:2px");
        }

        @Test
        @DisplayName("should show missing repository with MissingRepositoryInfo styling")
        void shouldShowMissingRepositoryWithInfoStyle() {
            // Given
            var aggregates = List.of(AggregateComponent.of("Product", "com.example.domain", 4, List.of(), List.of()));
            var components = ComponentDetails.of(aggregates, List.of(), List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.missingRepository("Product"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then
            assertThat(diagram).contains("class Product:::MissingRepositoryInfo");
            assertThat(diagram)
                    .contains(
                            "classDef MissingRepositoryInfo stroke:#1976D2,fill:#E3F2FD,color:#0D47A1,stroke-width:2px");
        }

        @Test
        @DisplayName("should show event naming violation with EventNamingWarning styling")
        void shouldShowEventNamingWithWarningStyle() {
            // Given
            var aggregates = List.of(AggregateComponent.of("Order", "com.example.domain", 5, List.of(), List.of()));
            var valueObjects = List.of(ValueObjectComponent.of("OrderEvent", "com.example.domain"));
            var components = ComponentDetails.of(aggregates, valueObjects, List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.eventNaming("OrderEvent"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then
            assertThat(diagram).contains("class OrderEvent:::EventNamingWarning");
            assertThat(diagram)
                    .contains("classDef EventNamingWarning stroke:#00ACC1,fill:#E0F7FA,color:#006064,stroke-width:2px");
        }

        @Test
        @DisplayName("should show port uncovered with PortUncoveredWarning styling")
        void shouldShowPortUncoveredWithWarningStyle() {
            // Given
            var aggregates = List.of(AggregateComponent.of("Order", "com.example.domain", 5, List.of(), List.of()));
            var valueObjects = List.of(ValueObjectComponent.of("PaymentGateway", "com.example.domain"));
            var components = ComponentDetails.of(aggregates, valueObjects, List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.portUncovered("PaymentGateway"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then
            assertThat(diagram).contains("class PaymentGateway:::PortUncoveredWarning");
            assertThat(diagram)
                    .contains(
                            "classDef PortUncoveredWarning stroke:#00897B,fill:#E0F2F1,color:#004D40,stroke-width:2px");
        }

        @Test
        @DisplayName("should show dependency inversion with DependencyInversionWarning styling")
        void shouldShowDependencyInversionWithWarningStyle() {
            // Given
            var aggregates =
                    List.of(AggregateComponent.of("OrderService", "com.example.domain", 5, List.of(), List.of()));
            var components = ComponentDetails.of(aggregates, List.of(), List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.dependencyInversion("OrderService"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then
            assertThat(diagram).contains("class OrderService:::DependencyInversionWarning");
            assertThat(diagram)
                    .contains(
                            "classDef DependencyInversionWarning stroke:#FFB300,fill:#FFF8E1,color:#FF6F00,stroke-width:2px");
        }

        @Test
        @DisplayName("should show layer violation with LayerViolationWarning styling")
        void shouldShowLayerViolationWithWarningStyle() {
            // Given
            var aggregates =
                    List.of(AggregateComponent.of("OrderAdapter", "com.example.adapter", 3, List.of(), List.of()));
            var components = ComponentDetails.of(aggregates, List.of(), List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.layerViolation("OrderAdapter"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then
            assertThat(diagram).contains("class OrderAdapter:::LayerViolationWarning");
            assertThat(diagram)
                    .contains(
                            "classDef LayerViolationWarning stroke:#616161,fill:#EEEEEE,color:#212121,stroke-width:2px");
        }

        @Test
        @DisplayName("should show port not interface with PortNotInterfaceWarning styling")
        void shouldShowPortNotInterfaceWithWarningStyle() {
            // Given
            var aggregates = List.of(AggregateComponent.of("Order", "com.example.domain", 5, List.of(), List.of()));
            var valueObjects = List.of(ValueObjectComponent.of("OrderPort", "com.example.domain"));
            var components = ComponentDetails.of(aggregates, valueObjects, List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(TypeViolation.portNotInterface("OrderPort"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then
            assertThat(diagram).contains("class OrderPort:::PortNotInterfaceWarning");
            assertThat(diagram)
                    .contains(
                            "classDef PortNotInterfaceWarning stroke:#8D6E63,fill:#EFEBE9,color:#4E342E,stroke-width:2px");
        }

        @Test
        @DisplayName("should prioritize higher severity styles over lower severity")
        void shouldPrioritizeHigherSeverityStylesOverLower() {
            // Given - Order has both CRITICAL (dependency inversion) and MINOR (event naming) violation
            var aggregates = List.of(AggregateComponent.of("Order", "com.example.domain", 5, List.of(), List.of()));
            var components = ComponentDetails.of(aggregates, List.of(), List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(
                    TypeViolation.eventNaming("Order"), // MINOR severity
                    TypeViolation.dependencyInversion("Order") // CRITICAL severity
                    );

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then - DependencyInversionWarning (CRITICAL) takes priority over EventNamingWarning (MINOR)
            assertThat(diagram).contains("class Order:::DependencyInversionWarning");
            assertThat(diagram).doesNotContain("class Order:::EventNamingWarning");
        }

        @Test
        @DisplayName("should handle multiple types with different violations")
        void shouldHandleMultipleTypesWithDifferentViolations() {
            // Given
            var aggregates = List.of(
                    AggregateComponent.of("Order", "com.example.domain", 5, List.of(), List.of()),
                    AggregateComponent.of("Customer", "com.example.domain", 3, List.of(), List.of()));
            var valueObjects = List.of(ValueObjectComponent.of("Money", "com.example.domain"));
            var components = ComponentDetails.of(aggregates, valueObjects, List.of(), List.of(), List.of(), List.of());
            var typeViolations = List.of(
                    TypeViolation.missingIdentity("Customer"),
                    TypeViolation.mutableValueObject("Money"),
                    TypeViolation.missingRepository("Order"));

            // When
            String diagram = builder.build(components, List.of(), typeViolations);

            // Then - Each type should have its own style
            assertThat(diagram).contains("class Customer:::MissingIdentityWarning");
            assertThat(diagram).contains("class Money:::MutableWarning");
            assertThat(diagram).contains("class Order:::MissingRepositoryInfo");
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
                    AggregateComponent.of("Order", "com.example.domain", 9, List.of(), List.of("OrderRepository")));
            var drivingPorts = List.of(
                    PortComponent.driving("OrderUseCase", "com.example.port", 8, false, null, List.of("Order")));
            var drivenPorts = List.of(
                    PortComponent.driven("OrderRepository", "com.example.port", "REPOSITORY", 6, true, "JpaOrderRepo"));
            var adapters = List.of(new AdapterComponent(
                    "JpaOrderRepo", "com.example.infra", "OrderRepository", AdapterComponent.AdapterType.DRIVEN));
            var components = ComponentDetails.of(aggregates, List.of(), List.of(), drivingPorts, drivenPorts, adapters);

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
            var components = ComponentDetails.of(
                    List.of(
                            AggregateComponent.of("Order", "pkg", 5, List.of(), List.of()),
                            AggregateComponent.of("Inventory", "pkg", 3, List.of(), List.of())),
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
    @DisplayName("ApplicationLayerDiagramBuilder")
    class ApplicationLayerDiagramBuilderTests {

        private final ApplicationLayerDiagramBuilder builder = new ApplicationLayerDiagramBuilder();

        @Test
        @DisplayName("should return null when no application layer components")
        void shouldReturnNullWhenNoApplicationLayerComponents() {
            // Given
            var components = ComponentDetails.empty();

            // When
            String diagram = builder.build(components, List.of(), List.of());

            // Then
            assertThat(diagram).isNull();
        }

        @Test
        @DisplayName("should generate diagram with application services")
        void shouldGenerateDiagramWithApplicationServices() {
            // Given
            var components = new ComponentDetails(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(ApplicationServiceComponent.of(
                            "OrderApplicationService",
                            "com.example.app",
                            5,
                            List.of("Order"),
                            List.of("OrderRepository"))),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());

            // When
            String diagram = builder.build(components, List.of(), List.of());

            // Then
            assertThat(diagram).contains("title: Application Layer");
            assertThat(diagram).contains("classDiagram");
            assertThat(diagram).contains("class OrderApplicationService{");
            assertThat(diagram).contains("<<ApplicationService>>");
            assertThat(diagram).contains("+methods 5");
            assertThat(diagram).contains("OrderApplicationService --> Order : orchestrates");
            assertThat(diagram).contains("OrderApplicationService --> OrderRepository : uses");
        }

        @Test
        @DisplayName("should generate diagram with command handlers")
        void shouldGenerateDiagramWithCommandHandlers() {
            // Given
            var components = new ComponentDetails(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(CommandHandlerComponent.of(
                            "PlaceOrderHandler", "com.example.app", "PlaceOrderCommand", "Order")),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());

            // When
            String diagram = builder.build(components, List.of(), List.of());

            // Then
            assertThat(diagram).contains("class PlaceOrderHandler{");
            assertThat(diagram).contains("<<CommandHandler>>");
            assertThat(diagram).contains("+handles PlaceOrderCommand");
            assertThat(diagram).contains("PlaceOrderHandler --> Order : modifies");
        }

        @Test
        @DisplayName("should generate diagram with query handlers")
        void shouldGenerateDiagramWithQueryHandlers() {
            // Given
            var components = new ComponentDetails(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(QueryHandlerComponent.of(
                            "GetOrdersHandler", "com.example.app", "GetOrdersQuery", "List~Order~")),
                    List.of(),
                    List.of(),
                    List.of());

            // When
            String diagram = builder.build(components, List.of(), List.of());

            // Then
            assertThat(diagram).contains("class GetOrdersHandler{");
            assertThat(diagram).contains("<<QueryHandler>>");
            assertThat(diagram).contains("+handles GetOrdersQuery");
            assertThat(diagram).contains("+returns List~Order~");
        }

        @Test
        @DisplayName("should apply dependency inversion violation styling")
        void shouldApplyDependencyInversionViolationStyling() {
            // Given
            var components = new ComponentDetails(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(ApplicationServiceComponent.of("OrderService", "com.example.app", 3)),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());
            var violations = List.of(TypeViolation.dependencyInversion("OrderService"));

            // When
            String diagram = builder.build(components, List.of(), violations);

            // Then
            assertThat(diagram).contains("class OrderService:::DependencyInversionWarning");
            assertThat(diagram).contains("classDef DependencyInversionWarning stroke:#FFB300");
        }
    }

    @Nested
    @DisplayName("PortsDiagramBuilder")
    class PortsDiagramBuilderTests {

        private final PortsDiagramBuilder builder = new PortsDiagramBuilder();

        @Test
        @DisplayName("should return null when no ports")
        void shouldReturnNullWhenNoPorts() {
            // Given
            var components = ComponentDetails.empty();

            // When
            String diagram = builder.build(components, List.of());

            // Then
            assertThat(diagram).isNull();
        }

        @Test
        @DisplayName("should generate diagram with driving ports")
        void shouldGenerateDiagramWithDrivingPorts() {
            // Given
            var components = new ComponentDetails(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(PortComponent.driving(
                            "OrderApi", "com.example.port", 5, true, "RestOrderController", List.of("Order"))),
                    List.of(),
                    List.of());

            // When
            String diagram = builder.build(components, List.of());

            // Then
            assertThat(diagram).contains("title: Ports Layer");
            assertThat(diagram).contains("classDiagram");
            assertThat(diagram).contains("class OrderApi{");
            assertThat(diagram).contains("<<DrivingPort>>");
            assertThat(diagram).contains("+methods 5");
            assertThat(diagram).contains("RestOrderController ..|> OrderApi : implements");
            assertThat(diagram).contains("OrderApi --> Order : orchestrates");
        }

        @Test
        @DisplayName("should generate diagram with driven ports")
        void shouldGenerateDiagramWithDrivenPorts() {
            // Given
            var components = new ComponentDetails(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(PortComponent.driven(
                            "OrderRepository", "com.example.port", "REPOSITORY", 4, true, "JpaOrderRepository")),
                    List.of());

            // When
            String diagram = builder.build(components, List.of());

            // Then
            assertThat(diagram).contains("class OrderRepository{");
            assertThat(diagram).contains("<<DrivenPort>>");
            assertThat(diagram).contains("+type REPOSITORY");
            assertThat(diagram).contains("+methods 4");
            assertThat(diagram).contains("JpaOrderRepository ..|> OrderRepository : implements");
        }

        @Test
        @DisplayName("should apply port uncovered violation styling")
        void shouldApplyPortUncoveredViolationStyling() {
            // Given
            var components = new ComponentDetails(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(PortComponent.driven("PaymentGateway", "com.example.port", "GATEWAY", 3, false, null)),
                    List.of());
            var violations = List.of(TypeViolation.portUncovered("PaymentGateway"));

            // When
            String diagram = builder.build(components, violations);

            // Then
            assertThat(diagram).contains("class PaymentGateway:::PortUncoveredWarning");
            assertThat(diagram).contains("classDef PortUncoveredWarning stroke:#00897B");
        }

        @Test
        @DisplayName("should apply port not interface violation styling")
        void shouldApplyPortNotInterfaceViolationStyling() {
            // Given
            var components = new ComponentDetails(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(PortComponent.driving("OrderPort", "com.example.port", 2, false, null, List.of())),
                    List.of(),
                    List.of());
            var violations = List.of(TypeViolation.portNotInterface("OrderPort"));

            // When
            String diagram = builder.build(components, violations);

            // Then
            assertThat(diagram).contains("class OrderPort:::PortNotInterfaceWarning");
            assertThat(diagram).contains("classDef PortNotInterfaceWarning stroke:#8D6E63");
        }
    }

    @Nested
    @DisplayName("DiagramGenerator")
    class DiagramGeneratorTests {

        @Test
        @DisplayName("should generate all 6 core diagrams")
        void shouldGenerateAllSixCoreDiagrams() {
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

        @Test
        @DisplayName("should generate optional application layer diagram when components exist")
        void shouldGenerateApplicationLayerDiagramWhenComponentsExist() {
            // Given
            var generator = new DiagramGenerator();
            var reportData = createReportDataWithApplicationLayer();

            // When
            DiagramSet diagrams = generator.generate(reportData, "Test Project");

            // Then
            assertThat(diagrams.applicationLayer()).isNotNull().contains("classDiagram");
            assertThat(diagrams.applicationLayer()).contains("<<ApplicationService>>");
        }

        @Test
        @DisplayName("should generate optional ports layer diagram when ports exist")
        void shouldGeneratePortsLayerDiagramWhenPortsExist() {
            // Given
            var generator = new DiagramGenerator();
            var reportData = createTestReportData();

            // When
            DiagramSet diagrams = generator.generate(reportData, "Test Project");

            // Then
            assertThat(diagrams.portsLayer()).isNotNull().contains("classDiagram");
            assertThat(diagrams.portsLayer()).contains("<<DrivingPort>>");
        }

        @Test
        @DisplayName("should return null for application layer when no components")
        void shouldReturnNullForApplicationLayerWhenNoComponents() {
            // Given
            var generator = new DiagramGenerator();
            var reportData = createReportDataWithoutApplicationLayer();

            // When
            DiagramSet diagrams = generator.generate(reportData, "Test Project");

            // Then
            assertThat(diagrams.applicationLayer()).isNull();
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
        var metadata = new ReportMetadata("Test", "1.0", java.time.Instant.now(), "10ms", "2.0.0", "2.0.0");

        var verdict = new Verdict(73, "C", ReportStatus.FAILED, "Test", "Summary", List.of(), ImmediateAction.none());

        var totals = new InventoryTotals(1, 0, 2, 1, 0, 1, 1);
        var inventory = new Inventory(List.of(new BoundedContextInventory("Test", 1, 0, 2, 0)), totals);
        var components = ComponentDetails.of(
                List.of(AggregateComponent.of("Order", "pkg", 5, List.of(), List.of())),
                List.of(ValueObjectComponent.of("Money", "pkg")),
                List.of(new IdentifierComponent("OrderId", "pkg", "UUID")),
                List.of(PortComponent.driving("OrderUseCase", "pkg", 3, false, null, List.of("Order"))),
                List.of(PortComponent.driven("OrderRepo", "pkg", "REPOSITORY", 4, false, null)),
                List.of());
        var architecture =
                new ArchitectureOverview("Test arch", inventory, components, DiagramsInfo.defaults(), List.of());

        var issues = IssuesSummary.empty();
        var remediation = RemediationPlan.empty();
        var appendix = new Appendix(
                createScoreBreakdown(78, 60, 100, 24, 100),
                List.of(),
                List.of(),
                List.of(new PackageMetric("pkg", 1, 1, 0.5, 0.0, 0.5, PackageMetric.ZoneType.STABLE_CORE)));

        return ReportData.create(metadata, verdict, architecture, issues, remediation, appendix);
    }

    private ReportData createReportDataWithApplicationLayer() {
        var metadata = new ReportMetadata("Test", "1.0", java.time.Instant.now(), "10ms", "2.0.0", "2.0.0");

        var verdict = new Verdict(73, "C", ReportStatus.FAILED, "Test", "Summary", List.of(), ImmediateAction.none());

        var totals = new InventoryTotals(1, 0, 2, 1, 0, 1, 1);
        var inventory = new Inventory(List.of(new BoundedContextInventory("Test", 1, 0, 2, 0)), totals);
        var components = new ComponentDetails(
                List.of(AggregateComponent.of("Order", "pkg", 5, List.of(), List.of())),
                List.of(),
                List.of(ValueObjectComponent.of("Money", "pkg")),
                List.of(new IdentifierComponent("OrderId", "pkg", "UUID")),
                List.of(),
                List.of(),
                List.of(ApplicationServiceComponent.of(
                        "OrderApplicationService", "pkg", 3, List.of("Order"), List.of())),
                List.of(),
                List.of(),
                List.of(PortComponent.driving("OrderUseCase", "pkg", 3, false, null, List.of("Order"))),
                List.of(PortComponent.driven("OrderRepo", "pkg", "REPOSITORY", 4, false, null)),
                List.of());
        var architecture =
                new ArchitectureOverview("Test arch", inventory, components, DiagramsInfo.defaults(), List.of());

        var issues = IssuesSummary.empty();
        var remediation = RemediationPlan.empty();
        var appendix = new Appendix(
                createScoreBreakdown(78, 60, 100, 24, 100),
                List.of(),
                List.of(),
                List.of(new PackageMetric("pkg", 1, 1, 0.5, 0.0, 0.5, PackageMetric.ZoneType.STABLE_CORE)));

        return ReportData.create(metadata, verdict, architecture, issues, remediation, appendix);
    }

    private ReportData createReportDataWithoutApplicationLayer() {
        var metadata = new ReportMetadata("Test", "1.0", java.time.Instant.now(), "10ms", "2.0.0", "2.0.0");

        var verdict = new Verdict(73, "C", ReportStatus.FAILED, "Test", "Summary", List.of(), ImmediateAction.none());

        var totals = new InventoryTotals(1, 0, 2, 1, 0, 0, 0);
        var inventory = new Inventory(List.of(new BoundedContextInventory("Test", 1, 0, 2, 0)), totals);
        var components = new ComponentDetails(
                List.of(AggregateComponent.of("Order", "pkg", 5, List.of(), List.of())),
                List.of(),
                List.of(ValueObjectComponent.of("Money", "pkg")),
                List.of(new IdentifierComponent("OrderId", "pkg", "UUID")),
                List.of(),
                List.of(),
                List.of(), // No application services
                List.of(), // No command handlers
                List.of(), // No query handlers
                List.of(), // No driving ports
                List.of(), // No driven ports
                List.of());
        var architecture =
                new ArchitectureOverview("Test arch", inventory, components, DiagramsInfo.defaults(), List.of());

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
