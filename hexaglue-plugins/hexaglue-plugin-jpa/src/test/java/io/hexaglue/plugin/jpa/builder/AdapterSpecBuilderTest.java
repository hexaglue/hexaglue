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

import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import io.hexaglue.plugin.jpa.strategy.AdapterContext;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.JavaConstruct;
import io.hexaglue.spi.ir.MethodKind;
import io.hexaglue.spi.ir.MethodParameter;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.spi.ir.PortMethod;
import io.hexaglue.spi.ir.SourceRef;
import io.hexaglue.spi.ir.TypeRef;
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
 * from ports and domain types, with special focus on method deduplication.
 *
 * @since 3.0.0
 */
@DisplayName("AdapterSpecBuilder")
class AdapterSpecBuilderTest {

    private static final String TEST_PKG = "com.example.domain";
    private static final String INFRA_PKG = "com.example.infrastructure.jpa";

    private JpaConfig config;
    private DomainType orderAggregate;

    @BeforeEach
    void setUp() {
        config = new JpaConfig("Entity", "Embeddable", "JpaRepository", "Adapter", "Mapper", "", false, false, true, true, true, true);
        orderAggregate = createOrderAggregate();
    }

    @Nested
    @DisplayName("build() with single port")
    class BuildWithSinglePort {

        @Test
        @DisplayName("should create adapter spec with correct class name")
        void shouldCreateAdapterSpecWithCorrectClassName() {
            // Given
            Port port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: Single port uses port name (Phase 1 change)
            assertThat(spec.className()).isEqualTo("OrderRepositoryAdapter");
        }

        @Test
        @DisplayName("should set correct package name")
        void shouldSetCorrectPackageName() {
            // Given
            Port port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
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
            Port port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
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
            Port port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
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
            Port port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
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
            Port port = createPortWithMethods("OrderRepository", List.of(
                    PortMethod.of("save", TypeRef.of(TEST_PKG + ".Order"), List.of(
                            MethodParameter.simple("order", TypeRef.of(TEST_PKG + ".Order"))), MethodKind.SAVE),
                    PortMethod.of("findById", TypeRef.of("java.util.Optional"), List.of(
                            MethodParameter.of("id", TypeRef.of("java.util.UUID"), true)), MethodKind.FIND_BY_ID),
                    PortMethod.of("findAll", TypeRef.of("java.util.List"), List.of(), MethodKind.FIND_ALL)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.methods()).hasSize(3);
            assertThat(spec.methods()).extracting(AdapterMethodSpec::name)
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
            Port port1 = createPortWithMethods("ReadableOrderRepository", List.of(
                    PortMethod.of("findById", TypeRef.of("java.util.Optional"), List.of(
                            MethodParameter.of("id", TypeRef.of("java.util.UUID"), true)), MethodKind.FIND_BY_ID)));

            Port port2 = createPortWithMethods("WritableOrderRepository", List.of(
                    PortMethod.of("findById", TypeRef.of("java.util.Optional"), List.of(
                            MethodParameter.of("id", TypeRef.of("java.util.UUID"), true)), MethodKind.FIND_BY_ID),
                    PortMethod.of("save", TypeRef.of(TEST_PKG + ".Order"), List.of(
                            MethodParameter.simple("order", TypeRef.of(TEST_PKG + ".Order"))), MethodKind.SAVE)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port1, port2))
                    .domainType(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: findById should appear only once
            assertThat(spec.methods()).hasSize(2);
            assertThat(spec.methods()).extracting(AdapterMethodSpec::name)
                    .containsExactly("findById", "save");

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
            Port port1 = createPortWithMethods("ReadableOrderRepository", List.of(
                    PortMethod.of("findById", TypeRef.of("java.util.Optional"), List.of(
                            MethodParameter.of("id", TypeRef.of("java.util.UUID"), true)), MethodKind.FIND_BY_ID)));

            Port port2 = createPortWithMethods("WritableOrderRepository", List.of(
                    PortMethod.of("save", TypeRef.of(TEST_PKG + ".Order"), List.of(
                            MethodParameter.simple("order", TypeRef.of(TEST_PKG + ".Order"))), MethodKind.SAVE)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port1, port2))
                    .domainType(orderAggregate)
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
            Port port1 = createPortWithMethods("PrimaryRepository", List.of(
                    PortMethod.of("findAll", TypeRef.of("java.util.List"), List.of(), MethodKind.FIND_ALL)));

            Port port2 = createPortWithMethods("SecondaryRepository", List.of(
                    PortMethod.of("findAll", TypeRef.of("java.util.List"), List.of(), MethodKind.FIND_ALL)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port1, port2))
                    .domainType(orderAggregate)
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
            Port port = createPortWithMethods("OrderRepository", List.of(
                    PortMethod.of("delete", TypeRef.of("void"), List.of(
                            MethodParameter.simple("order", TypeRef.of(TEST_PKG + ".Order"))), MethodKind.DELETE_ALL),
                    PortMethod.of("delete", TypeRef.of("void"), List.of(
                            MethodParameter.of("id", TypeRef.of("java.util.UUID"), true)), MethodKind.DELETE_BY_ID)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: Both methods should be kept (different signatures)
            assertThat(spec.methods()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("buildContext()")
    class BuildContext {

        @Test
        @DisplayName("should create context with correct field names")
        void shouldCreateContextWithCorrectFieldNames() {
            // Given
            Port port = createSimplePort("OrderRepository");

            // When
            AdapterContext context = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .buildContext();

            // Then
            assertThat(context.repositoryFieldName()).isEqualTo("repository");
            assertThat(context.mapperFieldName()).isEqualTo("mapper");
        }

        @Test
        @DisplayName("should create context with IdInfo for wrapped identity")
        void shouldCreateContextWithIdInfoForWrappedIdentity() {
            // Given: Aggregate with wrapped identity (OrderId wrapping UUID)
            Port port = createSimplePort("OrderRepository");

            // When
            AdapterContext context = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .buildContext();

            // Then: IdInfo should indicate wrapped identity
            assertThat(context.hasIdInfo()).isTrue();
            assertThat(context.hasWrappedId()).isTrue();
            assertThat(context.idInfo().wrappedType().toString()).isEqualTo(TEST_PKG + ".OrderId");
            assertThat(context.idInfo().unwrappedType().toString()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should create context with IdInfo for unwrapped identity")
        void shouldCreateContextWithIdInfoForUnwrappedIdentity() {
            // Given: Aggregate with unwrapped identity (raw UUID)
            DomainType entityWithRawId = createEntityWithRawId();
            Port port = createSimplePort("TaskRepository");

            // When
            AdapterContext context = AdapterSpecBuilder.builder()
                    .ports(List.of(port))
                    .domainType(entityWithRawId)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .buildContext();

            // Then: IdInfo should indicate unwrapped identity
            assertThat(context.hasIdInfo()).isTrue();
            assertThat(context.hasWrappedId()).isFalse();
            assertThat(context.idInfo().unwrappedType().toString()).isEqualTo("java.util.UUID");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should throw when ports is null")
        void shouldThrowWhenPortsIsNull() {
            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .ports(null)
                            .domainType(orderAggregate)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ports is required");
        }

        @Test
        @DisplayName("should throw when ports is empty")
        void shouldThrowWhenPortsIsEmpty() {
            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .ports(List.of())
                            .domainType(orderAggregate)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one port is required");
        }

        @Test
        @DisplayName("should throw when domainType is null")
        void shouldThrowWhenDomainTypeIsNull() {
            // Given
            Port port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .ports(List.of(port))
                            .domainType(null)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("domainType is required");
        }

        @Test
        @DisplayName("should throw when config is null")
        void shouldThrowWhenConfigIsNull() {
            // Given
            Port port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .ports(List.of(port))
                            .domainType(orderAggregate)
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
            Port port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .ports(List.of(port))
                            .domainType(orderAggregate)
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
            Port port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .ports(List.of(port))
                            .domainType(orderAggregate)
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
            Port readPort = createPortWithMethods("PokemonReadRepository", List.of(
                    PortMethod.of("findById", TypeRef.of("java.util.Optional"), List.of(
                            MethodParameter.of("id", TypeRef.of("java.lang.Integer"), true)), MethodKind.FIND_BY_ID),
                    PortMethod.of("findAll", TypeRef.of("java.util.List"), List.of(), MethodKind.FIND_ALL)));

            Port writePort = createPortWithMethods("PokemonWriteRepository", List.of(
                    PortMethod.of("save", TypeRef.of(TEST_PKG + ".Pokemon"), List.of(
                            MethodParameter.simple("pokemon", TypeRef.of(TEST_PKG + ".Pokemon"))), MethodKind.SAVE),
                    PortMethod.of("findById", TypeRef.of("java.util.Optional"), List.of(
                            MethodParameter.of("id", TypeRef.of("java.lang.Integer"), true)), MethodKind.FIND_BY_ID)));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .ports(List.of(readPort, writePort))
                    .domainType(orderAggregate)
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

    private DomainType createOrderAggregate() {
        return new DomainType(
                TEST_PKG + ".Order",
                "Order",
                TEST_PKG,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(Identity.wrapped(
                        "id",
                        TypeRef.of(TEST_PKG + ".OrderId"),
                        TypeRef.of("java.util.UUID"),
                        IdentityStrategy.ASSIGNED,
                        io.hexaglue.spi.ir.IdentityWrapperKind.RECORD,
                        "value")),
                List.of(),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private DomainType createEntityWithRawId() {
        return new DomainType(
                TEST_PKG + ".Task",
                "Task",
                TEST_PKG,
                DomainKind.ENTITY,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(Identity.unwrapped("id", TypeRef.of("java.util.UUID"), IdentityStrategy.ASSIGNED)),
                List.of(),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private Port createSimplePort(String name) {
        return new Port(
                TEST_PKG + ".ports.out." + name,
                name,
                TEST_PKG + ".ports.out",
                PortKind.REPOSITORY,
                PortDirection.DRIVEN,
                ConfidenceLevel.HIGH,
                List.of(TEST_PKG + ".Order"),
                TEST_PKG + ".Order",
                List.of(
                        PortMethod.of("save", TypeRef.of(TEST_PKG + ".Order"), List.of(
                                MethodParameter.simple("order", TypeRef.of(TEST_PKG + ".Order"))), MethodKind.SAVE)),
                List.of(),
                SourceRef.unknown());
    }

    private Port createPortWithMethods(String name, List<PortMethod> methods) {
        return new Port(
                TEST_PKG + ".ports.out." + name,
                name,
                TEST_PKG + ".ports.out",
                PortKind.REPOSITORY,
                PortDirection.DRIVEN,
                ConfidenceLevel.HIGH,
                List.of(TEST_PKG + ".Order"),
                TEST_PKG + ".Order",
                methods,
                List.of(),
                SourceRef.unknown());
    }
}
