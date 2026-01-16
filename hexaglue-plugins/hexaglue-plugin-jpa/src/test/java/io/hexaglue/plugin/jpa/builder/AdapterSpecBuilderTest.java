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

package io.hexaglue.plugin.jpa.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.PortClassification;
import io.hexaglue.arch.ports.PortOperation;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AdapterSpecBuilder}.
 *
 * <p>These tests validate the builder's ability to create adapter specifications
 * from v4 DrivenPorts and DomainEntities, with special focus on method deduplication.
 *
 * @since 4.0.0
 */
@DisplayName("AdapterSpecBuilder")
class AdapterSpecBuilderTest {

    private static final String TEST_PKG = "com.example.domain";
    private static final String INFRA_PKG = "com.example.infrastructure.jpa";

    private JpaConfig config;
    private DomainEntity orderAggregate;

    @BeforeEach
    void setUp() {
        config = new JpaConfig(
                "Entity", "Embeddable", "JpaRepository", "Adapter", "Mapper", "", false, false, true, true, true, true);
        orderAggregate = createOrderAggregate();
    }

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @Nested
    @DisplayName("build() with single port")
    class BuildWithSinglePort {

        @Test
        @DisplayName("should create adapter spec with correct class name")
        void shouldCreateAdapterSpecWithCorrectClassName() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: Single port uses port name
            assertThat(spec.className()).isEqualTo("OrderRepositoryAdapter");
        }

        @Test
        @DisplayName("should set correct package name")
        void shouldSetCorrectPackageName() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.packageName()).isEqualTo(INFRA_PKG);
        }

        @Test
        @DisplayName("should resolve correct entity type")
        void shouldResolveCorrectEntityType() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.entityClass().toString()).isEqualTo(INFRA_PKG + ".OrderEntity");
        }

        @Test
        @DisplayName("should resolve correct repository type")
        void shouldResolveCorrectRepositoryType() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.repositoryClass().toString()).isEqualTo(INFRA_PKG + ".OrderJpaRepository");
        }

        @Test
        @DisplayName("should resolve correct mapper type")
        void shouldResolveCorrectMapperType() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.mapperClass().toString()).isEqualTo(INFRA_PKG + ".OrderMapper");
        }

        @Test
        @DisplayName("should include all methods from port")
        void shouldIncludeAllMethodsFromPort() {
            // Given
            DrivenPort port = createPortWithOperations(
                    "OrderRepository",
                    List.of(
                            new PortOperation(
                                    "save",
                                    TypeRef.of(TEST_PKG + ".Order"),
                                    List.of(TypeRef.of(TEST_PKG + ".Order")),
                                    null),
                            new PortOperation(
                                    "findById",
                                    TypeRef.of("java.util.Optional"),
                                    List.of(TypeRef.of("java.util.UUID")),
                                    null),
                            new PortOperation("findAll", TypeRef.of("java.util.List"), List.of(), null)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.methods()).hasSize(3);
            assertThat(spec.methods())
                    .extracting(AdapterMethodSpec::name)
                    .containsExactly("save", "findById", "findAll");
        }
    }

    @Nested
    @DisplayName("build() with multiple ports - deduplication")
    class BuildWithMultiplePortsDeduplication {

        @Test
        @DisplayName("should deduplicate identical methods from multiple ports")
        void shouldDeduplicateIdenticalMethodsFromMultiplePorts() {
            // Given: Two ports with the same findById method (common in sample-pokedex)
            DrivenPort port1 = createPortWithOperations(
                    "ReadableOrderRepository",
                    List.of(new PortOperation(
                            "findById",
                            TypeRef.of("java.util.Optional"),
                            List.of(TypeRef.of("java.util.UUID")),
                            null)));

            DrivenPort port2 = createPortWithOperations(
                    "WritableOrderRepository",
                    List.of(
                            new PortOperation(
                                    "findById",
                                    TypeRef.of("java.util.Optional"),
                                    List.of(TypeRef.of("java.util.UUID")),
                                    null),
                            new PortOperation(
                                    "save",
                                    TypeRef.of(TEST_PKG + ".Order"),
                                    List.of(TypeRef.of(TEST_PKG + ".Order")),
                                    null)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port1, port2))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: findById should appear only once
            assertThat(spec.methods()).hasSize(2);
            assertThat(spec.methods()).extracting(AdapterMethodSpec::name).containsExactly("findById", "save");

            // Verify only one findById
            long findByIdCount = spec.methods().stream()
                    .filter(m -> m.name().equals("findById"))
                    .count();
            assertThat(findByIdCount)
                    .as("findById should appear exactly once after deduplication")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should implement all ports in the adapter")
        void shouldImplementAllPortsInTheAdapter() {
            // Given
            DrivenPort port1 = createPortWithOperations(
                    "ReadableOrderRepository",
                    List.of(new PortOperation(
                            "findById",
                            TypeRef.of("java.util.Optional"),
                            List.of(TypeRef.of("java.util.UUID")),
                            null)));

            DrivenPort port2 = createPortWithOperations(
                    "WritableOrderRepository",
                    List.of(new PortOperation(
                            "save", TypeRef.of(TEST_PKG + ".Order"), List.of(TypeRef.of(TEST_PKG + ".Order")), null)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port1, port2))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: Adapter should implement both ports
            assertThat(spec.implementedPorts()).hasSize(2);
        }

        @Test
        @DisplayName("should keep first occurrence when deduplicating")
        void shouldKeepFirstOccurrenceWhenDeduplicating() {
            // Given: Two ports with same method name but potentially different return types
            // (first port's version should be kept)
            DrivenPort port1 = createPortWithOperations(
                    "PrimaryRepository",
                    List.of(new PortOperation("findAll", TypeRef.of("java.util.List"), List.of(), null)));

            DrivenPort port2 = createPortWithOperations(
                    "SecondaryRepository",
                    List.of(new PortOperation("findAll", TypeRef.of("java.util.List"), List.of(), null)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port1, port2))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.methods()).hasSize(1);
            assertThat(spec.methods().get(0).name()).isEqualTo("findAll");
        }

        @Test
        @DisplayName("should not deduplicate methods with different parameter types")
        void shouldNotDeduplicateMethodsWithDifferentParameterTypes() {
            // Given: Same method name but different parameter types (overloading)
            // Note: null returnType means void
            DrivenPort port = createPortWithOperations(
                    "OrderRepository",
                    List.of(
                            new PortOperation("delete", null, List.of(TypeRef.of(TEST_PKG + ".Order")), null),
                            new PortOperation("delete", null, List.of(TypeRef.of("java.util.UUID")), null)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: Both methods should be kept (different signatures)
            assertThat(spec.methods()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should throw when drivenPorts is null")
        void shouldThrowWhenDrivenPortsIsNull() {
            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(null)
                            .domainEntity(orderAggregate)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("drivenPorts is required");
        }

        @Test
        @DisplayName("should throw when drivenPorts is empty")
        void shouldThrowWhenDrivenPortsIsEmpty() {
            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(List.of())
                            .domainEntity(orderAggregate)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one driven port is required");
        }

        @Test
        @DisplayName("should throw when domainEntity is null")
        void shouldThrowWhenDomainEntityIsNull() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(List.of(port))
                            .domainEntity(null)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("domainEntity is required");
        }

        @Test
        @DisplayName("should throw when config is null")
        void shouldThrowWhenConfigIsNull() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(List.of(port))
                            .domainEntity(orderAggregate)
                            .config(null)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("config is required");
        }

        @Test
        @DisplayName("should throw when infrastructurePackage is null")
        void shouldThrowWhenInfrastructurePackageIsNull() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(List.of(port))
                            .domainEntity(orderAggregate)
                            .config(config)
                            .infrastructurePackage(null)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("infrastructurePackage is required");
        }

        @Test
        @DisplayName("should throw when infrastructurePackage is empty")
        void shouldThrowWhenInfrastructurePackageIsEmpty() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(List.of(port))
                            .domainEntity(orderAggregate)
                            .config(config)
                            .infrastructurePackage("")
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("infrastructurePackage is required");
        }
    }

    @Nested
    @DisplayName("Critical bug fix: sample-pokedex duplicate methods")
    class CriticalBugFixSamplePokedex {

        @Test
        @DisplayName("merged ports should not produce duplicate findById methods")
        void mergedPortsShouldNotProduceDuplicateFindByIdMethods() {
            // Given: This was causing "Duplicate method 'findById'" errors in sample-pokedex
            DrivenPort readPort = createPortWithOperations(
                    "PokemonReadRepository",
                    List.of(
                            new PortOperation(
                                    "findById",
                                    TypeRef.of("java.util.Optional"),
                                    List.of(TypeRef.of("java.lang.Integer")),
                                    null),
                            new PortOperation("findAll", TypeRef.of("java.util.List"), List.of(), null)));

            DrivenPort writePort = createPortWithOperations(
                    "PokemonWriteRepository",
                    List.of(
                            new PortOperation(
                                    "save",
                                    TypeRef.of(TEST_PKG + ".Pokemon"),
                                    List.of(TypeRef.of(TEST_PKG + ".Pokemon")),
                                    null),
                            new PortOperation(
                                    "findById",
                                    TypeRef.of("java.util.Optional"),
                                    List.of(TypeRef.of("java.lang.Integer")),
                                    null)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(readPort, writePort))
                    .domainEntity(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: No duplicate findById
            long findByIdCount = spec.methods().stream()
                    .filter(m -> m.name().equals("findById"))
                    .count();
            assertThat(findByIdCount)
                    .as("findById should appear exactly once after port merging")
                    .isEqualTo(1);

            // And: All unique methods should be present
            assertThat(spec.methods()).hasSize(3); // findById, findAll, save
        }
    }

    // ===== Helper Methods =====

    private DomainEntity createOrderAggregate() {
        return new DomainEntity(
                ElementId.of(TEST_PKG + ".Order"),
                ElementKind.AGGREGATE_ROOT,
                "id",
                TypeRef.of(TEST_PKG + ".OrderId"),
                Optional.empty(),
                List.of(),
                null,
                highConfidence(ElementKind.AGGREGATE_ROOT));
    }

    private DrivenPort createSimplePort(String name) {
        return new DrivenPort(
                ElementId.of(TEST_PKG + ".ports.out." + name),
                PortClassification.REPOSITORY,
                List.of(new PortOperation(
                        "save", TypeRef.of(TEST_PKG + ".Order"), List.of(TypeRef.of(TEST_PKG + ".Order")), null)),
                Optional.empty(),
                List.of(),
                null,
                highConfidence(ElementKind.DRIVEN_PORT));
    }

    private DrivenPort createPortWithOperations(String name, List<PortOperation> operations) {
        return new DrivenPort(
                ElementId.of(TEST_PKG + ".ports.out." + name),
                PortClassification.REPOSITORY,
                operations,
                Optional.empty(),
                List.of(),
                null,
                highConfidence(ElementKind.DRIVEN_PORT));
    }
}
