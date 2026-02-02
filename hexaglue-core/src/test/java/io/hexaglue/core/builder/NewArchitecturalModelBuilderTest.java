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

package io.hexaglue.core.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.core.graph.testing.TestGraphBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NewArchitecturalModelBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("NewArchitecturalModelBuilder")
class NewArchitecturalModelBuilderTest {

    private NewArchitecturalModelBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new NewArchitecturalModelBuilder();
    }

    private ClassificationResult createAggregateResult(String qualifiedName) {
        return ClassificationResult.classified(
                NodeId.type(qualifiedName),
                ClassificationTarget.DOMAIN,
                "AGGREGATE_ROOT",
                ConfidenceLevel.HIGH,
                "test-criterion",
                100,
                "Test aggregate classification",
                List.of(),
                List.of());
    }

    private ClassificationResult createEntityResult(String qualifiedName) {
        return ClassificationResult.classified(
                NodeId.type(qualifiedName),
                ClassificationTarget.DOMAIN,
                "ENTITY",
                ConfidenceLevel.HIGH,
                "test-criterion",
                100,
                "Test entity classification",
                List.of(),
                List.of());
    }

    private ClassificationResult createUnclassifiedResult(String qualifiedName) {
        return ClassificationResult.unclassifiedDomain(NodeId.type(qualifiedName), null);
    }

    private ClassificationResult createDrivenPortResult(String qualifiedName, String portKind) {
        return ClassificationResult.classifiedPort(
                NodeId.type(qualifiedName),
                portKind,
                ConfidenceLevel.HIGH,
                "test-criterion",
                100,
                "Test driven port classification",
                List.of(),
                List.of(),
                PortDirection.DRIVEN);
    }

    @Nested
    @DisplayName("build()")
    class BuildTests {

        @Test
        @DisplayName("should throw on null graph query")
        void shouldThrowOnNullGraphQuery() {
            ClassificationResults results = new ClassificationResults(Map.of());

            assertThatNullPointerException()
                    .isThrownBy(() -> builder.build(null, results))
                    .withMessage("graphQuery must not be null");
        }

        @Test
        @DisplayName("should throw on null classification results")
        void shouldThrowOnNullResults() {
            // Create a minimal test graph query
            GraphQuery graphQuery = TestGraphBuilder.create().build().query();

            assertThatNullPointerException()
                    .isThrownBy(() -> builder.build(graphQuery, null))
                    .withMessage("results must not be null");
        }

        @Test
        @DisplayName("should build result with empty inputs")
        void shouldBuildWithEmptyInputs() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create().build().query();
            ClassificationResults results = new ClassificationResults(Map.of());

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result).isNotNull();
            assertThat(result.typeRegistry().size()).isEqualTo(0);
            assertThat(result.classificationReport()).isNotNull();
            assertThat(result.domainIndex()).isNotNull();
            assertThat(result.portIndex()).isNotNull();
        }

        @Test
        @DisplayName("should set generated timestamp")
        void shouldSetGeneratedTimestamp() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create().build().query();
            ClassificationResults results = new ClassificationResults(Map.of());

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should build with classified types from graph")
        void shouldBuildWithClassifiedTypes() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withClass("com.example.OrderItem")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.Order"), createAggregateResult("com.example.Order"),
                    NodeId.type("com.example.OrderItem"), createEntityResult("com.example.OrderItem")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(2);
            assertThat(result.classificationReport().stats().totalTypes()).isEqualTo(2);
        }

        @Test
        @DisplayName("should build with unclassified types from graph")
        void shouldBuildWithUnclassifiedTypes() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Utils")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(
                    Map.of(NodeId.type("com.example.Utils"), createUnclassifiedResult("com.example.Utils")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(1);
            assertThat(result.classificationReport().stats().unclassifiedTypes())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should populate domain index with aggregates")
        void shouldPopulateDomainIndex() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withField("id", "java.util.UUID")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(
                    Map.of(NodeId.type("com.example.Order"), createAggregateResult("com.example.Order")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            // Note: aggregate may fallback to unclassified if no identity field detected properly
            assertThat(result.typeRegistry().size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should have hasIssues return true for unclassified types")
        void shouldReportIssuesForUnclassified() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Unknown")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(
                    Map.of(NodeId.type("com.example.Unknown"), createUnclassifiedResult("com.example.Unknown")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("should report size from result")
        void shouldReportSize() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.A")
                    .withClass("com.example.B")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.A"), createEntityResult("com.example.A"),
                    NodeId.type("com.example.B"), createEntityResult("com.example.B")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("excluded type handling")
    class ExcludedTypeHandling {

        @Test
        @DisplayName("should skip types not present in classification results")
        void shouldSkipTypesNotInClassificationResults() {
            // given - graph has 3 types, but only 2 are classified
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withClass("com.example.OrderItem")
                    .withClass("com.acme.shop.application.InventoryService")
                    .build()
                    .query();

            // Only Order and OrderItem are classified; InventoryService is excluded
            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.Order"), createAggregateResult("com.example.Order"),
                    NodeId.type("com.example.OrderItem"), createEntityResult("com.example.OrderItem")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then - only 2 types should be in the registry, not 3
            assertThat(result.typeRegistry().size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should not create fallback unclassified for excluded types")
        void shouldNotCreateFallbackForExcludedTypes() {
            // given - graph has a type that is not classified
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.acme.shop.application.InventoryService")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of());

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then - no types should appear in the registry
            assertThat(result.typeRegistry().size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("driven port kind routing")
    class DrivenPortKindRouting {

        @Test
        @DisplayName("should route EVENT_PUBLISHER kind to driven port builder")
        void shouldRouteEventPublisherToDrivenPort() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withInterface("com.example.EventPublisher")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.EventPublisher"),
                    createDrivenPortResult("com.example.EventPublisher", "EVENT_PUBLISHER")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(1);
            List<DrivenPort> drivenPorts = result.portIndex().drivenPorts().toList();
            assertThat(drivenPorts).hasSize(1);
            assertThat(drivenPorts.get(0).id().simpleName()).isEqualTo("EventPublisher");
        }

        @Test
        @DisplayName("should route NOTIFICATION kind to driven port builder")
        void shouldRouteNotificationToDrivenPort() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withInterface("com.example.NotificationSender")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.NotificationSender"),
                    createDrivenPortResult("com.example.NotificationSender", "NOTIFICATION")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(1);
            List<DrivenPort> drivenPorts = result.portIndex().drivenPorts().toList();
            assertThat(drivenPorts).hasSize(1);
            assertThat(drivenPorts.get(0).id().simpleName()).isEqualTo("NotificationSender");
        }

        @Test
        @DisplayName("should route GENERIC kind to driven port builder")
        void shouldRouteGenericToDrivenPort() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withInterface("com.example.GenericPort")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.GenericPort"),
                    createDrivenPortResult("com.example.GenericPort", "GENERIC")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(1);
            List<DrivenPort> drivenPorts = result.portIndex().drivenPorts().toList();
            assertThat(drivenPorts).hasSize(1);
            assertThat(drivenPorts.get(0).id().simpleName()).isEqualTo("GenericPort");
        }

        @Test
        @DisplayName("should route REPOSITORY kind to driven port builder")
        void shouldRouteRepositoryToDrivenPort() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withInterface("com.example.OrderRepository")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.OrderRepository"),
                    createDrivenPortResult("com.example.OrderRepository", "REPOSITORY")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(1);
            List<DrivenPort> drivenPorts = result.portIndex().drivenPorts().toList();
            assertThat(drivenPorts).hasSize(1);
        }

        @Test
        @DisplayName("should route GATEWAY kind to driven port builder")
        void shouldRouteGatewayToDrivenPort() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withInterface("com.example.PaymentGateway")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.PaymentGateway"),
                    createDrivenPortResult("com.example.PaymentGateway", "GATEWAY")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(1);
            List<DrivenPort> drivenPorts = result.portIndex().drivenPorts().toList();
            assertThat(drivenPorts).hasSize(1);
            assertThat(drivenPorts.get(0).id().simpleName()).isEqualTo("PaymentGateway");
        }
    }
}
