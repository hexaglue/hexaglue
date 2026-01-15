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

package io.hexaglue.arch.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.builder.ArchitecturalModelBuilder;
import io.hexaglue.syntax.SyntaxProvider;
import io.hexaglue.syntax.spoon.SpoonSyntaxProvider;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the classification pipeline.
 *
 * <p>These tests validate the complete flow from source code parsing
 * through classification to ArchitecturalModel construction.</p>
 */
@DisplayName("Classification Integration")
class ClassificationIntegrationTest {

    private static final String BASE_PACKAGE = "io.hexaglue.arch.integration.fixtures";
    private static ArchitecturalModel model;

    @BeforeAll
    static void setUp() {
        // Parse the fixtures using Spoon
        SyntaxProvider syntaxProvider = SpoonSyntaxProvider.builder()
                .basePackage(BASE_PACKAGE)
                .sourceDirectory(Path.of("src/test/java/io/hexaglue/arch/integration/fixtures"))
                .build();

        // Build the architectural model
        model = ArchitecturalModelBuilder.builder(syntaxProvider)
                .projectName("Integration Test")
                .basePackage(BASE_PACKAGE)
                .build();
    }

    @Nested
    @DisplayName("Domain Classification")
    class DomainClassificationTest {

        @Test
        @DisplayName("should classify @AggregateRoot as AGGREGATE_ROOT")
        void shouldClassifyAggregateRoot() {
            var aggregateRoots = model.domainEntities()
                    .filter(e -> e.isAggregateRoot())
                    .toList();

            assertThat(aggregateRoots).hasSize(1);
            assertThat(aggregateRoots.get(0).id().qualifiedName())
                    .isEqualTo("io.hexaglue.arch.integration.fixtures.domain.Order");
            assertThat(aggregateRoots.get(0).kind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should classify @Identifier as IDENTIFIER")
        void shouldClassifyIdentifiers() {
            var identifiers = model.identifiers().toList();

            assertThat(identifiers).hasSize(2);
            assertThat(identifiers)
                    .extracting(id -> id.id().simpleName())
                    .containsExactlyInAnyOrder("OrderId", "CustomerId");
        }

        @Test
        @DisplayName("should classify @ValueObject as VALUE_OBJECT")
        void shouldClassifyValueObjects() {
            var valueObjects = model.valueObjects().toList();

            // Money and Address have explicit @ValueObject annotation
            assertThat(valueObjects)
                    .extracting(vo -> vo.id().simpleName())
                    .contains("Money", "Address");
        }

        @Test
        @DisplayName("should classify @DomainEvent as DOMAIN_EVENT")
        void shouldClassifyDomainEvents() {
            var events = model.domainEvents().toList();

            assertThat(events).hasSize(1);
            assertThat(events.get(0).id().simpleName()).isEqualTo("OrderPlaced");
        }
    }

    @Nested
    @DisplayName("Port Classification")
    class PortClassificationTest {

        @Test
        @DisplayName("should classify @DrivingPort as DRIVING_PORT")
        void shouldClassifyDrivingPorts() {
            var drivingPorts = model.drivingPorts().toList();

            assertThat(drivingPorts)
                    .extracting(p -> p.id().simpleName())
                    .contains("PlaceOrderUseCase");
        }

        @Test
        @DisplayName("should classify @DrivenPort as DRIVEN_PORT")
        void shouldClassifyExplicitDrivenPorts() {
            var drivenPorts = model.drivenPorts().toList();

            assertThat(drivenPorts)
                    .extracting(p -> p.id().simpleName())
                    .contains("PaymentGateway");
        }

        @Test
        @DisplayName("should classify *Repository by naming convention")
        void shouldClassifyRepositoryByNaming() {
            var drivenPorts = model.drivenPorts().toList();

            assertThat(drivenPorts)
                    .extracting(p -> p.id().simpleName())
                    .contains("OrderRepository");
        }
    }

    @Nested
    @DisplayName("Unclassified Types")
    class UnclassifiedTypesTest {

        @Test
        @DisplayName("should have unclassified types for non-annotated classes")
        void shouldHaveUnclassifiedTypes() {
            var unclassified = model.unclassifiedTypes().toList();

            // OrderLine (class without annotation) and OrderStatus (enum) should be unclassified
            // Plus the annotation definitions themselves
            assertThat(unclassified).isNotEmpty();
        }

        @Test
        @DisplayName("should track OrderLine as unclassified")
        void shouldTrackOrderLineAsUnclassified() {
            var orderLine = model.unclassifiedTypes()
                    .filter(u -> u.id().simpleName().equals("OrderLine"))
                    .findFirst();

            assertThat(orderLine).isPresent();
        }

        @Test
        @DisplayName("should track OrderStatus enum as unclassified")
        void shouldTrackOrderStatusAsUnclassified() {
            var orderStatus = model.unclassifiedTypes()
                    .filter(u -> u.id().simpleName().equals("OrderStatus"))
                    .findFirst();

            assertThat(orderStatus).isPresent();
        }
    }

    @Nested
    @DisplayName("Classification Traces")
    class ClassificationTracesTest {

        @Test
        @DisplayName("should provide classification trace for aggregate root")
        void shouldProvideTraceForAggregateRoot() {
            var order = model.domainEntities()
                    .filter(e -> e.id().simpleName().equals("Order"))
                    .findFirst()
                    .orElseThrow();

            assertThat(order.classificationTrace()).isNotNull();
            assertThat(order.classificationTrace().classifiedAs()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(order.classificationTrace().explain()).contains("AggregateRoot");
        }

        @Test
        @DisplayName("should provide classification trace for driven port")
        void shouldProvideTraceForDrivenPort() {
            var repository = model.drivenPorts()
                    .filter(p -> p.id().simpleName().equals("OrderRepository"))
                    .findFirst()
                    .orElseThrow();

            assertThat(repository.classificationTrace()).isNotNull();
            assertThat(repository.classificationTrace().classifiedAs()).isEqualTo(ElementKind.DRIVEN_PORT);
        }
    }

    @Nested
    @DisplayName("Metadata")
    class MetadataTest {

        @Test
        @DisplayName("should have correct parser name")
        void shouldHaveCorrectParserName() {
            assertThat(model.analysisMetadata().parserName()).isEqualTo("Spoon");
        }

        @Test
        @DisplayName("should count analyzed types")
        void shouldCountAnalyzedTypes() {
            // We have: domain (10 types) + ports (5 types) = 15+ types
            assertThat(model.analysisMetadata().typesAnalyzed()).isGreaterThanOrEqualTo(15);
        }

        @Test
        @DisplayName("should have valid analysis duration")
        void shouldHaveValidDuration() {
            assertThat(model.analysisMetadata().duration()).isNotNull();
            assertThat(model.analysisMetadata().durationMillis()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Model Statistics")
    class ModelStatisticsTest {

        @Test
        @DisplayName("should report correct model size")
        void shouldReportCorrectSize() {
            // All classified + unclassified types
            assertThat(model.size()).isGreaterThanOrEqualTo(15);
        }

        @Test
        @DisplayName("should report unclassified count")
        void shouldReportUnclassifiedCount() {
            // At least OrderLine, OrderStatus, and annotation types
            assertThat(model.unclassifiedCount()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should report hasUnclassified correctly")
        void shouldReportHasUnclassified() {
            assertThat(model.hasUnclassified()).isTrue();
        }
    }
}
