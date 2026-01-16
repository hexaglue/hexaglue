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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for C4DiagramBuilder.
 *
 * <p>Note: Tests that require ArchitecturalModel are currently disabled pending
 * creation of test fixtures for the v4 model. See Phase 3 of the SPI fusion refactoring.
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
    @Disabled("Pending ArchitecturalModel test fixtures - see Phase 3 of SPI fusion refactoring")
    class ContainerDiagramTests {

        @Test
        @DisplayName("should generate container diagram with ports")
        void shouldGenerateContainerDiagramWithPorts() {
            // TODO: Requires ArchitecturalModel test fixture
        }

        @Test
        @DisplayName("should separate driving and driven ports")
        void shouldSeparateDrivingAndDrivenPorts() {
            // TODO: Requires ArchitecturalModel test fixture
        }
    }

    @Nested
    @DisplayName("Aggregate Diagram")
    @Disabled("Pending ArchitecturalModel test fixtures - see Phase 3 of SPI fusion refactoring")
    class AggregateDiagramTests {

        @Test
        @DisplayName("should generate aggregate diagram with relationships")
        void shouldGenerateAggregateDiagram() {
            // TODO: Requires ArchitecturalModel test fixture
        }

        @Test
        @DisplayName("should handle no aggregates")
        void shouldHandleNoAggregates() {
            // TODO: Requires ArchitecturalModel test fixture
        }

        @Test
        @DisplayName("should group entities with their aggregate")
        void shouldGroupEntitiesWithAggregate() {
            // TODO: Requires ArchitecturalModel test fixture
        }
    }

    @Nested
    @DisplayName("Port Matrix Diagram")
    @Disabled("Pending ArchitecturalModel test fixtures - see Phase 3 of SPI fusion refactoring")
    class PortMatrixDiagramTests {

        @Test
        @DisplayName("should generate port matrix with icons")
        void shouldGeneratePortMatrixWithIcons() {
            // TODO: Requires ArchitecturalModel test fixture
        }

        @Test
        @DisplayName("should show repository icon for repositories")
        void shouldShowRepositoryIcon() {
            // TODO: Requires ArchitecturalModel test fixture
        }

        @Test
        @DisplayName("should handle empty ports")
        void shouldHandleEmptyPorts() {
            // TODO: Requires ArchitecturalModel test fixture
        }
    }
}
