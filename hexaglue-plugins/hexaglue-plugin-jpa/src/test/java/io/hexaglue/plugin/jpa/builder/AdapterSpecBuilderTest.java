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
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AdapterSpecBuilder}.
 *
 * <p>These tests validate the builder's ability to create adapter specifications
 * from V5 DrivenPorts and AggregateRoots, with special focus on method deduplication.
 *
 * @since 4.0.0
 */
@DisplayName("AdapterSpecBuilder")
class AdapterSpecBuilderTest {

    private static final String TEST_PKG = "com.example.domain";
    private static final String INFRA_PKG = "com.example.infrastructure.jpa";

    private JpaConfig config;
    private AggregateRoot orderAggregate;

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
                    .aggregateRoot(orderAggregate)
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
                    .aggregateRoot(orderAggregate)
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
                    .aggregateRoot(orderAggregate)
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
                    .aggregateRoot(orderAggregate)
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
                    .aggregateRoot(orderAggregate)
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
                            operationToMethod(
                                    "save", TypeRef.of(TEST_PKG + ".Order"), List.of(TypeRef.of(TEST_PKG + ".Order"))),
                            operationToMethod(
                                    "findById",
                                    TypeRef.of("java.util.Optional"),
                                    List.of(TypeRef.of("java.util.UUID"))),
                            operationToMethod("findAll", TypeRef.of("java.util.List"), List.of())));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .aggregateRoot(orderAggregate)
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
                    List.of(operationToMethod(
                            "findById", TypeRef.of("java.util.Optional"), List.of(TypeRef.of("java.util.UUID")))));

            DrivenPort port2 = createPortWithOperations(
                    "WritableOrderRepository",
                    List.of(
                            operationToMethod(
                                    "findById",
                                    TypeRef.of("java.util.Optional"),
                                    List.of(TypeRef.of("java.util.UUID"))),
                            operationToMethod(
                                    "save",
                                    TypeRef.of(TEST_PKG + ".Order"),
                                    List.of(TypeRef.of(TEST_PKG + ".Order")))));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port1, port2))
                    .aggregateRoot(orderAggregate)
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
                    List.of(operationToMethod(
                            "findById", TypeRef.of("java.util.Optional"), List.of(TypeRef.of("java.util.UUID")))));

            DrivenPort port2 = createPortWithOperations(
                    "WritableOrderRepository",
                    List.of(operationToMethod(
                            "save", TypeRef.of(TEST_PKG + ".Order"), List.of(TypeRef.of(TEST_PKG + ".Order")))));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port1, port2))
                    .aggregateRoot(orderAggregate)
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
                    List.of(operationToMethod("findAll", TypeRef.of("java.util.List"), List.of())));

            DrivenPort port2 = createPortWithOperations(
                    "SecondaryRepository",
                    List.of(operationToMethod("findAll", TypeRef.of("java.util.List"), List.of())));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port1, port2))
                    .aggregateRoot(orderAggregate)
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
                            operationToMethod("delete", null, List.of(TypeRef.of(TEST_PKG + ".Order"))),
                            operationToMethod("delete", null, List.of(TypeRef.of("java.util.UUID")))));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .aggregateRoot(orderAggregate)
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
                            .aggregateRoot(orderAggregate)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("drivenPorts is required");
        }

        @Test
        @DisplayName("should throw when drivenPorts is empty")
        void shouldThrowWhenDrivenPortsIsEmpty() {
            // When/Then: validateRequiredFields throws IllegalStateException for empty ports
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(List.of())
                            .aggregateRoot(orderAggregate)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("drivenPorts is required");
        }

        @Test
        @DisplayName("should throw when domainEntity is null")
        void shouldThrowWhenDomainEntityIsNull() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(List.of(port))
                            .aggregateRoot(null)
                            .config(config)
                            .infrastructurePackage(INFRA_PKG)
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("aggregateRoot or entity is required");
        }

        @Test
        @DisplayName("should throw when config is null")
        void shouldThrowWhenConfigIsNull() {
            // Given
            DrivenPort port = createSimplePort("OrderRepository");

            // When/Then
            assertThatThrownBy(() -> AdapterSpecBuilder.builder()
                            .drivenPorts(List.of(port))
                            .aggregateRoot(orderAggregate)
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
                            .aggregateRoot(orderAggregate)
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
                            .aggregateRoot(orderAggregate)
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
                            operationToMethod(
                                    "findById",
                                    TypeRef.of("java.util.Optional"),
                                    List.of(TypeRef.of("java.lang.Integer"))),
                            operationToMethod("findAll", TypeRef.of("java.util.List"), List.of())));

            DrivenPort writePort = createPortWithOperations(
                    "PokemonWriteRepository",
                    List.of(
                            operationToMethod(
                                    "save",
                                    TypeRef.of(TEST_PKG + ".Pokemon"),
                                    List.of(TypeRef.of(TEST_PKG + ".Pokemon"))),
                            operationToMethod(
                                    "findById",
                                    TypeRef.of("java.util.Optional"),
                                    List.of(TypeRef.of("java.lang.Integer")))));

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(readPort, writePort))
                    .aggregateRoot(orderAggregate)
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

    @Nested
    @DisplayName("Bug fix: double suffix in adapter naming")
    class DoubleSuffixBugFix {

        @Test
        @DisplayName("should not produce double 'Repository' in adapter class name")
        void shouldNotProduceDoubleRepositoryInAdapterClassName() {
            // Given: suffix "RepositoryAdapter" and port name "CustomerRepository"
            // Expected: "CustomerRepositoryAdapter", NOT "CustomerRepositoryRepositoryAdapter"
            JpaConfig configWithRepositoryAdapter = new JpaConfig(
                    "Entity", "Embeddable", "JpaRepository", "RepositoryAdapter", "Mapper", "", false, false, true,
                    true, true, true);

            DrivenPort port = createSimplePort("CustomerRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .aggregateRoot(orderAggregate)
                    .config(configWithRepositoryAdapter)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.className())
                    .as("Should merge overlapping 'Repository' segment")
                    .isEqualTo("CustomerRepositoryAdapter");
        }

        @Test
        @DisplayName("should handle non-overlapping suffix normally")
        void shouldHandleNonOverlappingSuffixNormally() {
            // Given: suffix "Adapter" and port name "OrderRepository"
            // Expected: "OrderRepositoryAdapter" (simple concatenation)
            DrivenPort port = createSimplePort("OrderRepository");

            // When
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port))
                    .aggregateRoot(orderAggregate)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.className()).isEqualTo("OrderRepositoryAdapter");
        }

        @Test
        @DisplayName("should not produce double suffix for multi-port adapter")
        void shouldNotProduceDoubleSuffixForMultiPortAdapter() {
            // Given: multi-port case uses aggregate name, suffix "RepositoryAdapter"
            JpaConfig configWithRepositoryAdapter = new JpaConfig(
                    "Entity", "Embeddable", "JpaRepository", "RepositoryAdapter", "Mapper", "", false, false, true,
                    true, true, true);

            DrivenPort port1 = createSimplePort("ReadableOrderRepository");
            DrivenPort port2 = createSimplePort("WritableOrderRepository");

            // When: multi-port uses aggregate name "Order" + suffix "RepositoryAdapter"
            AdapterSpec spec = AdapterSpecBuilder.builder()
                    .drivenPorts(List.of(port1, port2))
                    .aggregateRoot(orderAggregate)
                    .config(configWithRepositoryAdapter)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: "Order" + "RepositoryAdapter" = "OrderRepositoryAdapter" (no overlap)
            assertThat(spec.className()).isEqualTo("OrderRepositoryAdapter");
        }
    }

    // ===== Helper Methods =====

    private AggregateRoot createOrderAggregate() {
        // Create identity field
        Field identityField = Field.builder("id", TypeRef.of(TEST_PKG + ".OrderId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        // Create basic structure for Order aggregate
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(identityField))
                .build();

        return AggregateRoot.builder(
                        TypeId.of(TEST_PKG + ".Order"),
                        structure,
                        highConfidence(ElementKind.AGGREGATE_ROOT),
                        identityField)
                .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                .build();
    }

    private DrivenPort createSimplePort(String name) {
        Method saveMethod = new Method(
                "save",
                TypeRef.of(TEST_PKG + ".Order"),
                List.of(Parameter.of("order", TypeRef.of(TEST_PKG + ".Order"))),
                Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                List.of(),
                Optional.empty(),
                List.of(),
                Set.of(),
                OptionalInt.empty(),
                Optional.empty());

        TypeStructure structure = TypeStructure.builder(TypeNature.INTERFACE)
                .modifiers(Set.of(Modifier.PUBLIC))
                .methods(List.of(saveMethod))
                .build();

        return DrivenPort.repository(
                TypeId.of(TEST_PKG + ".ports.out." + name),
                structure,
                highConfidence(ElementKind.DRIVEN_PORT),
                TypeRef.of(TEST_PKG + ".Order"));
    }

    private DrivenPort createPortWithOperations(String name, List<Method> methods) {
        TypeStructure structure = TypeStructure.builder(TypeNature.INTERFACE)
                .modifiers(Set.of(Modifier.PUBLIC))
                .methods(methods)
                .build();

        return DrivenPort.repository(
                TypeId.of(TEST_PKG + ".ports.out." + name),
                structure,
                highConfidence(ElementKind.DRIVEN_PORT),
                TypeRef.of(TEST_PKG + ".Order"));
    }

    // Helper to convert old-style operations to V5 methods
    private Method operationToMethod(String name, TypeRef returnType, List<TypeRef> paramTypes) {
        List<Parameter> params = paramTypes.isEmpty()
                ? List.of()
                : List.of(Parameter.of("param" + (paramTypes.size() > 1 ? "0" : ""), paramTypes.get(0)));

        return new Method(
                name,
                returnType != null ? returnType : TypeRef.of("void"),
                params,
                Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                List.of(),
                Optional.empty(),
                List.of(),
                Set.of(),
                OptionalInt.empty(),
                Optional.empty());
    }
}
