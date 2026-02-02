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

package io.hexaglue.core.classification.port.criteria;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for port classification criteria.
 */
class PortCriteriaTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(true, analyzer);
    }

    // =========================================================================
    // Explicit Annotation Criteria
    // =========================================================================

    @Nested
    class ExplicitRepositoryCriteriaTest {

        @Test
        void shouldMatchInterfaceWithRepositoryAnnotation() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitRepositoryCriteria criteria = new ExplicitRepositoryCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("@Repository");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVEN);
            assertThat(criteria.priority()).isEqualTo(100);
        }

        @Test
        void shouldNotMatchInterfaceWithoutAnnotation() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitRepositoryCriteria criteria = new ExplicitRepositoryCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class ExplicitPrimaryPortCriteriaTest {

        @Test
        void shouldMatchInterfaceImplementingPrimaryPort() throws IOException {
            writeSource("com/example/OrderingCoffee.java", """
                    package com.example;
                    import org.jmolecules.architecture.hexagonal.PrimaryPort;
                    public interface OrderingCoffee extends PrimaryPort {
                        void placeOrder(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase = graph.typeNode("com.example.OrderingCoffee").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitPrimaryPortCriteria criteria = new ExplicitPrimaryPortCriteria();
            MatchResult result = criteria.evaluate(useCase, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("PrimaryPort");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.USE_CASE);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVING);
        }
    }

    @Nested
    class ExplicitSecondaryPortCriteriaTest {

        @Test
        void shouldMatchInterfaceImplementingSecondaryPort() throws IOException {
            writeSource("com/example/PaymentGateway.java", """
                    package com.example;
                    import org.jmolecules.architecture.hexagonal.SecondaryPort;
                    public interface PaymentGateway extends SecondaryPort {
                        void processPayment(Object payment);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode gateway = graph.typeNode("com.example.PaymentGateway").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitSecondaryPortCriteria criteria = new ExplicitSecondaryPortCriteria();
            MatchResult result = criteria.evaluate(gateway, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("SecondaryPort");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.GATEWAY);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVEN);
        }
    }

    // =========================================================================
    // Semantic Driving Port Criteria (M9 - Marker Interface Exclusion)
    // =========================================================================

    @Nested
    class SemanticDrivingPortCriteriaMarkerInterfaceTest {

        @Test
        void shouldNotMatchMarkerInterface() throws IOException {
            // Marker interface without methods (like DomainEvent)
            writeSource("com/example/DomainEvent.java", """
                    package com.example;
                    public interface DomainEvent {
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode markerInterface = graph.typeNode("com.example.DomainEvent").orElseThrow();
            GraphQuery query = graph.query();

            // Verify it's detected as an interface
            assertThat(markerInterface.isInterface()).isTrue();

            // Verify it has no methods
            assertThat(query.methodsOf(markerInterface)).isEmpty();

            // Create InterfaceFacts directly to simulate "implementedByCore = true"
            // This tests the specific behavior without needing the full classification pipeline
            io.hexaglue.core.classification.semantic.InterfaceFacts markerFacts =
                    io.hexaglue.core.classification.semantic.InterfaceFacts.drivingPort(markerInterface.id(), 1, false);

            // Verify the facts indicate DRIVING port candidate
            assertThat(markerFacts.implementedByCore()).isTrue();

            // Build InterfaceFactsIndex with our custom facts
            io.hexaglue.core.classification.semantic.InterfaceFactsIndex factsIndex =
                    io.hexaglue.core.classification.semantic.InterfaceFactsIndex.fromFacts(
                            java.util.List.of(markerFacts));

            SemanticDrivingPortCriteria criteria = new SemanticDrivingPortCriteria(factsIndex);
            MatchResult result = criteria.evaluate(markerInterface, query);

            // Marker interface should NOT be classified as DRIVING port
            // even though implementedByCore is true
            assertThat(result.matched())
                    .as("Marker interface (no methods) should not be classified as DRIVING port")
                    .isFalse();
        }

        @Test
        void shouldMatchInterfaceWithMethods() throws IOException {
            // Interface with business methods
            writeSource("com/example/ports/in/OrderUseCase.java", """
                    package com.example.ports.in;
                    public interface OrderUseCase {
                        void createOrder(String customerId);
                        void cancelOrder(String orderId);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase =
                    graph.typeNode("com.example.ports.in.OrderUseCase").orElseThrow();
            GraphQuery query = graph.query();

            // Verify it has methods
            assertThat(query.methodsOf(useCase)).isNotEmpty();

            // Create InterfaceFacts directly to simulate "implementedByCore = true"
            io.hexaglue.core.classification.semantic.InterfaceFacts useCaseFacts =
                    io.hexaglue.core.classification.semantic.InterfaceFacts.drivingPort(useCase.id(), 1, true);

            // Build InterfaceFactsIndex with our custom facts
            io.hexaglue.core.classification.semantic.InterfaceFactsIndex factsIndex =
                    io.hexaglue.core.classification.semantic.InterfaceFactsIndex.fromFacts(
                            java.util.List.of(useCaseFacts));

            SemanticDrivingPortCriteria criteria = new SemanticDrivingPortCriteria(factsIndex);
            MatchResult result = criteria.evaluate(useCase, query);

            // Interface with methods should be classified as DRIVING port
            assertThat(result.matched())
                    .as("Interface with methods should be classified as DRIVING port when implemented by CoreAppClass")
                    .isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVING);
        }

        @Test
        void shouldNotMatchDomainEventInterface() throws IOException {
            // DomainEvent interface in domain package (with methods)
            writeSource("com/example/domain/shared/DomainEvent.java", """
                    package com.example.domain.shared;
                    import java.time.Instant;
                    import java.util.UUID;
                    public interface DomainEvent {
                        UUID eventId();
                        Instant occurredAt();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode domainEvent =
                    graph.typeNode("com.example.domain.shared.DomainEvent").orElseThrow();
            GraphQuery query = graph.query();

            // Verify it has methods (not a marker interface)
            assertThat(query.methodsOf(domainEvent)).isNotEmpty();

            // Create InterfaceFacts to simulate "implementedByCore = true"
            io.hexaglue.core.classification.semantic.InterfaceFacts eventFacts =
                    io.hexaglue.core.classification.semantic.InterfaceFacts.drivingPort(domainEvent.id(), 1, false);

            // Build InterfaceFactsIndex
            io.hexaglue.core.classification.semantic.InterfaceFactsIndex factsIndex =
                    io.hexaglue.core.classification.semantic.InterfaceFactsIndex.fromFacts(
                            java.util.List.of(eventFacts));

            SemanticDrivingPortCriteria criteria = new SemanticDrivingPortCriteria(factsIndex);
            MatchResult result = criteria.evaluate(domainEvent, query);

            // DomainEvent should NOT be classified as DRIVING port
            // even though it has methods and implementedByCore is true
            assertThat(result.matched())
                    .as("DomainEvent interface should not be classified as DRIVING port")
                    .isFalse();
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private ApplicationGraph buildGraph() {
        return buildGraph("com.example");
    }

    private ApplicationGraph buildGraph(String basePackage) {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, basePackage, false);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of(basePackage, 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
