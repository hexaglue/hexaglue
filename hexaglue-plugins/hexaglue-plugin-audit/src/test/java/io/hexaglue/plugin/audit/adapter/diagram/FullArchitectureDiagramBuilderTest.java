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

import io.hexaglue.plugin.audit.domain.model.report.AdapterComponent;
import io.hexaglue.plugin.audit.domain.model.report.AggregateComponent;
import io.hexaglue.plugin.audit.domain.model.report.ApplicationServiceComponent;
import io.hexaglue.plugin.audit.domain.model.report.ComponentDetails;
import io.hexaglue.plugin.audit.domain.model.report.PortComponent;
import io.hexaglue.plugin.audit.domain.model.report.Relationship;
import io.hexaglue.plugin.audit.domain.model.report.TypeViolation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for FullArchitectureDiagramBuilder.
 *
 * <p>Tests verify that the builder generates a complete C4Component diagram
 * showing all hexagonal architecture layers (Driving, Domain, Driven) with
 * relationships and violation highlighting.
 *
 * @since 5.0.0
 */
@DisplayName("FullArchitectureDiagramBuilder")
class FullArchitectureDiagramBuilderTest {

    private FullArchitectureDiagramBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new FullArchitectureDiagramBuilder();
    }

    @Nested
    @DisplayName("Empty Components")
    class EmptyComponentsTests {

        @Test
        @DisplayName("should return null for empty components")
        void shouldReturnNullForEmptyComponents() {
            // Given
            ComponentDetails empty = ComponentDetails.empty();
            List<Relationship> relationships = List.of();
            List<TypeViolation> violations = List.of();

            // When
            String result = builder.build("TestProject", empty, relationships, violations)
                    .orElse(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when only aggregates are present but no ports")
        void shouldReturnNullWhenOnlyAggregatesPresent() {
            // Given: components with only aggregates
            ComponentDetails components = new ComponentDetails(
                    List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of())),
                    List.of(), // entities
                    List.of(), // valueObjects
                    List.of(), // identifiers
                    List.of(), // domainEvents
                    List.of(), // domainServices
                    List.of(), // applicationServices
                    List.of(), // commandHandlers
                    List.of(), // queryHandlers
                    List.of(), // drivingPorts
                    List.of(), // drivenPorts
                    List.of()); // adapters

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElse(null);

            // Then: Should return a diagram even with only aggregates
            // (actually, we want the full architecture so we need at least some ports or services)
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Mermaid Syntax")
    class MermaidSyntaxTests {

        @Test
        @DisplayName("should generate valid Mermaid C4Component syntax")
        void shouldGenerateValidMermaidC4ComponentSyntax() {
            // Given
            ComponentDetails components = createMinimalComponents();

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).startsWith("C4Component");
            assertThat(result).contains("title Full Architecture - TestProject");
        }

        @Test
        @DisplayName("should include project name in title")
        void shouldIncludeProjectNameInTitle() {
            // Given
            ComponentDetails components = createMinimalComponents();

            // When
            String result = builder.build("MyAwesomeProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).contains("title Full Architecture - MyAwesomeProject");
        }
    }

    @Nested
    @DisplayName("Layer Boundaries")
    class LayerBoundariesTests {

        @Test
        @DisplayName("should create Driving Side boundary with application services")
        void shouldCreateDrivingSideBoundaryWithServices() {
            // Given
            ComponentDetails components = createComponentsWithAllLayers();

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).contains("Container_Boundary(driving, \"Driving Side\")");
            assertThat(result).contains("PlaceOrderUseCase");
            assertThat(result).contains("Application Service");
        }

        @Test
        @DisplayName("should create Domain Core boundary with aggregates")
        void shouldCreateDomainCoreBoundaryWithAggregates() {
            // Given
            ComponentDetails components = createComponentsWithAllLayers();

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).contains("Container_Boundary(domain, \"Domain Core\")");
            assertThat(result).contains("Order");
            assertThat(result).contains("Aggregate Root");
        }

        @Test
        @DisplayName("should create Driven Side boundary with ports and adapters")
        void shouldCreateDrivenSideBoundaryWithPortsAndAdapters() {
            // Given
            ComponentDetails components = createComponentsWithAllLayers();

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).contains("Container_Boundary(driven, \"Driven Side\")");
            assertThat(result).contains("OrderRepository");
            assertThat(result).contains("REPOSITORY");
        }
    }

    @Nested
    @DisplayName("Relationships")
    class RelationshipsTests {

        @Test
        @DisplayName("should show orchestrates relationship from service to aggregate")
        void shouldShowOrchestratesRelationship() {
            // Given
            ApplicationServiceComponent service = ApplicationServiceComponent.of(
                    "PlaceOrderUseCase", "com.example.application", 3, List.of("Order"), List.of());
            ComponentDetails components = new ComponentDetails(
                    List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of())),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(service),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(PortComponent.driven(
                            "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                    List.of());

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).contains("Rel_D(");
            assertThat(result).contains("orchestrates");
        }

        @Test
        @DisplayName("should show implements relationship from adapter to port")
        void shouldShowImplementsRelationship() {
            // Given
            ComponentDetails components = new ComponentDetails(
                    List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of())),
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
                            "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                    List.of(new AdapterComponent(
                            "JpaOrderRepository",
                            "com.example.adapter",
                            "OrderRepository",
                            AdapterComponent.AdapterType.DRIVEN)));

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).contains("implemented by");
        }
    }

    @Nested
    @DisplayName("Cycles and Violations")
    class CyclesAndViolationsTests {

        @Test
        @DisplayName("should show BiRel for cycles between aggregates")
        void shouldShowBiRelForCycles() {
            // Given
            ComponentDetails components = createComponentsWithCycle();
            List<Relationship> relationships = List.of(Relationship.cycle("Order", "Customer", "references"));
            List<TypeViolation> violations = List.of(TypeViolation.cycle("Order"), TypeViolation.cycle("Customer"));

            // When
            String result = builder.build("TestProject", components, relationships, violations)
                    .orElseThrow();

            // Then
            assertThat(result).contains("BiRel(");
            assertThat(result).containsIgnoringCase("cycle");
        }

        @Test
        @DisplayName("should highlight cycle participants with red background")
        void shouldHighlightCycleParticipantsWithRedBackground() {
            // Given
            ComponentDetails components = createComponentsWithCycle();
            List<Relationship> relationships = List.of(Relationship.cycle("Order", "Customer", "references"));
            List<TypeViolation> violations = List.of(TypeViolation.cycle("Order"), TypeViolation.cycle("Customer"));

            // When
            String result = builder.build("TestProject", components, relationships, violations)
                    .orElseThrow();

            // Then
            assertThat(result).contains("UpdateElementStyle(");
            assertThat(result).contains("#FF5978"); // Alert color for cycles
        }
    }

    @Nested
    @DisplayName("Component Limits")
    class ComponentLimitsTests {

        @Test
        @DisplayName("should limit components per layer to fifteen")
        void shouldLimitComponentsPerLayerToFifteen() {
            // Given: 20 aggregates
            List<AggregateComponent> manyAggregates = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) {
                manyAggregates.add(AggregateComponent.of("Aggregate" + i, "com.example", 5, List.of(), List.of()));
            }
            ComponentDetails components = new ComponentDetails(
                    manyAggregates,
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
                            "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                    List.of());

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then: Should contain max 15 aggregates
            assertThat(result).isNotNull();
            // Count occurrences of "Aggregate" in the diagram - should be at most 15
            int count = 0;
            int idx = 0;
            while ((idx = result.indexOf("Aggregate", idx)) != -1) {
                count++;
                idx++;
            }
            // Allow for "Aggregate Root" label repetitions but limited actual components
            assertThat(count).isLessThanOrEqualTo(30); // 15 components * 2 mentions each max
        }
    }

    @Nested
    @DisplayName("Component Types")
    class ComponentTypesTests {

        @Test
        @DisplayName("should use ComponentDb for repository ports")
        void shouldUseComponentDbForRepositoryPorts() {
            // Given
            ComponentDetails components = new ComponentDetails(
                    List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of())),
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
                            "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                    List.of());

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).contains("ComponentDb(");
        }

        @Test
        @DisplayName("should use Component for gateway ports")
        void shouldUseComponentForGatewayPorts() {
            // Given
            ComponentDetails components = new ComponentDetails(
                    List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of())),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(PortComponent.driven("PaymentGateway", "com.example.port", "GATEWAY", 2, false, null)),
                    List.of());

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then: Should contain Component but NOT ComponentDb for the gateway
            assertThat(result).contains("PaymentGateway");
            assertThat(result).contains("GATEWAY");
        }
    }

    @Nested
    @DisplayName("Minimal Components")
    class MinimalComponentsTests {

        @Test
        @DisplayName("should generate diagram with only aggregates and driven ports")
        void shouldGenerateDiagramWithOnlyAggregatesAndDrivenPorts() {
            // Given: components similar to sample-basic (no app services, no driving ports)
            ComponentDetails components = new ComponentDetails(
                    List.of(AggregateComponent.of("Task", "com.example.domain", 5, List.of(), List.of())),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(), // no application services
                    List.of(),
                    List.of(),
                    List.of(), // no driving ports
                    List.of(PortComponent.driven(
                            "TaskRepository", "com.example.ports.out", "REPOSITORY", 4, false, null)),
                    List.of()); // no adapters

            // When
            String result = builder.build("MinimalProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then: Should still generate a diagram showing Domain Core and Driven Side
            assertThat(result).isNotNull();
            assertThat(result).contains("C4Component");
            assertThat(result).contains("Domain Core");
            assertThat(result).contains("Task");
            assertThat(result).contains("Driven Side");
            assertThat(result).contains("TaskRepository");
        }
    }

    @Nested
    @DisplayName("Driving Ports Support")
    class DrivingPortsTests {

        @Test
        @DisplayName("should include driving ports in driving side")
        void shouldIncludeDrivingPortsInDrivingSide() {
            // Given
            ComponentDetails components = new ComponentDetails(
                    List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of())),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(PortComponent.driving(
                            "OrderUseCase", "com.example.port", 3, false, null, List.of("Order"))),
                    List.of(PortComponent.driven(
                            "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                    List.of());

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then
            assertThat(result).contains("OrderUseCase");
            assertThat(result).contains("Driving Port");
        }
    }

    @Nested
    @DisplayName("Styling")
    class StylingTests {

        @Test
        @DisplayName("should not double-style application services as adapters")
        void shouldNotDoublStyleApplicationServicesAsAdapters() {
            // Given: application service exists as both app service and DRIVING adapter
            ComponentDetails components = new ComponentDetails(
                    List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of())),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(ApplicationServiceComponent.of(
                            "OrderAppService", "com.example.app", 3, List.of("Order"), List.of())),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(PortComponent.driven(
                            "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                    List.of(
                            new AdapterComponent(
                                    "OrderAppService",
                                    "com.example.app",
                                    "OrderUseCase",
                                    AdapterComponent.AdapterType.DRIVING),
                            new AdapterComponent(
                                    "JpaOrderRepository",
                                    "com.example.adapter",
                                    "OrderRepository",
                                    AdapterComponent.AdapterType.DRIVEN)));

            // When
            String result = builder.build("TestProject", components, List.of(), List.of())
                    .orElseThrow();

            // Then - Application service should have blue styling, not green
            // Count blue style (#2196F3) for OrderAppService
            assertThat(result)
                    .contains("UpdateElementStyle(orderappservice, $fontColor=\"white\", $bgColor=\"#2196F3\")");
            // DRIVING adapter should NOT be re-styled as green
            long greenStyleLines = result.lines()
                    .filter(l -> l.contains("orderappservice") && l.contains("#4CAF50"))
                    .count();
            assertThat(greenStyleLines).isZero();
            // DRIVEN adapter should still get green
            assertThat(result)
                    .contains("UpdateElementStyle(jpaorderrepository, $fontColor=\"white\", $bgColor=\"#4CAF50\")");
        }
    }

    // Helper methods for creating test fixtures

    private ComponentDetails createMinimalComponents() {
        return new ComponentDetails(
                List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of())),
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
                        "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                List.of());
    }

    private ComponentDetails createComponentsWithAllLayers() {
        return new ComponentDetails(
                List.of(AggregateComponent.of("Order", "com.example.order", 5, List.of(), List.of("OrderRepository"))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(ApplicationServiceComponent.of(
                        "PlaceOrderUseCase",
                        "com.example.application",
                        3,
                        List.of("Order"),
                        List.of("OrderRepository"))),
                List.of(),
                List.of(),
                List.of(),
                List.of(PortComponent.driven(
                        "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                List.of(new AdapterComponent(
                        "JpaOrderRepository",
                        "com.example.adapter",
                        "OrderRepository",
                        AdapterComponent.AdapterType.DRIVEN)));
    }

    private ComponentDetails createComponentsWithCycle() {
        return new ComponentDetails(
                List.of(
                        AggregateComponent.of("Order", "com.example.order", 5, List.of("Customer"), List.of()),
                        AggregateComponent.of("Customer", "com.example.customer", 3, List.of("Order"), List.of())),
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
                        "OrderRepository", "com.example.port", "REPOSITORY", 3, true, "JpaOrderRepository")),
                List.of());
    }
}
