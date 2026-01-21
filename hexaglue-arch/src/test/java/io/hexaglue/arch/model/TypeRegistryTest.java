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

package io.hexaglue.arch.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeRegistry}.
 *
 * @since 4.1.0
 */
@DisplayName("TypeRegistry")
class TypeRegistryTest {

    private static final TypeId ORDER_ID = TypeId.of("com.example.Order");
    private static final TypeId ITEM_ID = TypeId.of("com.example.Item");
    private static final TypeId MONEY_ID = TypeId.of("com.example.Money");
    private static final TypeId UTILS_ID = TypeId.of("com.example.Utils");
    private static final TypeId PORT_ID = TypeId.of("com.example.OrderPort");

    private TypeStructure defaultStructure;
    private ClassificationTrace defaultTrace;
    private Field idField;

    @BeforeEach
    void setUp() {
        defaultStructure = TypeStructure.builder(TypeNature.CLASS).build();
        defaultTrace = ClassificationTrace.highConfidence(
                io.hexaglue.arch.ElementKind.AGGREGATE_ROOT, "test-criterion", "Test classification");
        idField = Field.of("id", TypeRef.of("java.util.UUID"));
    }

    private AggregateRoot createAggregate(TypeId id) {
        return AggregateRoot.builder(id, defaultStructure, defaultTrace, idField)
                .build();
    }

    private Entity createEntity(TypeId id) {
        ClassificationTrace entityTrace =
                ClassificationTrace.highConfidence(io.hexaglue.arch.ElementKind.ENTITY, "test-criterion", "Test");
        return Entity.of(id, defaultStructure, entityTrace, idField);
    }

    private ValueObject createValueObject(TypeId id) {
        ClassificationTrace voTrace =
                ClassificationTrace.highConfidence(io.hexaglue.arch.ElementKind.VALUE_OBJECT, "test-criterion", "Test");
        return new ValueObject(id, defaultStructure, voTrace);
    }

    private DrivingPort createDrivingPort(TypeId id) {
        ClassificationTrace portTrace =
                ClassificationTrace.highConfidence(io.hexaglue.arch.ElementKind.DRIVING_PORT, "test-criterion", "Test");
        TypeStructure interfaceStructure =
                TypeStructure.builder(TypeNature.INTERFACE).build();
        return DrivingPort.of(id, interfaceStructure, portTrace);
    }

    private UnclassifiedType createUnclassified(TypeId id) {
        ClassificationTrace unclassifiedTrace = ClassificationTrace.unclassified("Test unclassified", List.of());
        return UnclassifiedType.of(id, defaultStructure, unclassifiedTrace, UnclassifiedCategory.UNKNOWN);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build empty registry")
        void shouldBuildEmptyRegistry() {
            // when
            TypeRegistry registry = TypeRegistry.builder().build();

            // then
            assertThat(registry.size()).isEqualTo(0);
            assertThat(registry.all().count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should add single type")
        void shouldAddSingleType() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);

            // when
            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            // then
            assertThat(registry.size()).isEqualTo(1);
            assertThat(registry.get(ORDER_ID)).contains(order);
        }

        @Test
        @DisplayName("should add multiple types")
        void shouldAddMultipleTypes() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            ValueObject money = createValueObject(MONEY_ID);

            // when
            TypeRegistry registry = TypeRegistry.builder().add(order).add(money).build();

            // then
            assertThat(registry.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should add all types from collection")
        void shouldAddAllFromCollection() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            ValueObject money = createValueObject(MONEY_ID);

            // when
            TypeRegistry registry =
                    TypeRegistry.builder().addAll(List.of(order, money)).build();

            // then
            assertThat(registry.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should reject null type")
        void shouldRejectNullType() {
            assertThatThrownBy(() -> TypeRegistry.builder().add(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject duplicate type id")
        void shouldRejectDuplicateTypeId() {
            // given
            AggregateRoot order1 = createAggregate(ORDER_ID);
            AggregateRoot order2 = createAggregate(ORDER_ID);

            // then
            assertThatThrownBy(
                            () -> TypeRegistry.builder().add(order1).add(order2).build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate type id");
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("should return type by id")
        void shouldReturnTypeById() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            // when
            Optional<ArchType> result = registry.get(ORDER_ID);

            // then
            assertThat(result).contains(order);
        }

        @Test
        @DisplayName("should return empty for unknown id")
        void shouldReturnEmptyForUnknownId() {
            // given
            TypeRegistry registry = TypeRegistry.builder().build();

            // when
            Optional<ArchType> result = registry.get(ORDER_ID);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return typed optional with correct type")
        void shouldReturnTypedOptional() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            // when
            Optional<AggregateRoot> result = registry.get(ORDER_ID, AggregateRoot.class);

            // then
            assertThat(result).contains(order);
        }

        @Test
        @DisplayName("should return empty for wrong type")
        void shouldReturnEmptyForWrongType() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            // when
            Optional<Entity> result = registry.get(ORDER_ID, Entity.class);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("all()")
    class AllTests {

        @Test
        @DisplayName("should return all types")
        void shouldReturnAllTypes() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            ValueObject money = createValueObject(MONEY_ID);
            TypeRegistry registry = TypeRegistry.builder().add(order).add(money).build();

            // when
            List<ArchType> all = registry.all().toList();

            // then
            assertThat(all).containsExactlyInAnyOrder(order, money);
        }

        @Test
        @DisplayName("should return all types of specific class")
        void shouldReturnAllOfSpecificClass() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            ValueObject money = createValueObject(MONEY_ID);
            Entity item = createEntity(ITEM_ID);
            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(money).add(item).build();

            // when
            List<AggregateRoot> aggregates = registry.all(AggregateRoot.class).toList();
            List<DomainType> domainTypes = registry.all(DomainType.class).toList();

            // then
            assertThat(aggregates).containsExactly(order);
            assertThat(domainTypes).containsExactlyInAnyOrder(order, money, item);
        }
    }

    @Nested
    @DisplayName("ofKind()")
    class OfKindTests {

        @Test
        @DisplayName("should return types of specific kind")
        void shouldReturnTypesOfKind() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            ValueObject money = createValueObject(MONEY_ID);
            DrivingPort port = createDrivingPort(PORT_ID);
            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(money).add(port).build();

            // when
            List<ArchType> aggregates = registry.ofKind(ArchKind.AGGREGATE_ROOT).toList();
            List<ArchType> valueObjects = registry.ofKind(ArchKind.VALUE_OBJECT).toList();
            List<ArchType> drivingPorts = registry.ofKind(ArchKind.DRIVING_PORT).toList();

            // then
            assertThat(aggregates).containsExactly(order);
            assertThat(valueObjects).containsExactly(money);
            assertThat(drivingPorts).containsExactly(port);
        }

        @Test
        @DisplayName("should return empty for no matches")
        void shouldReturnEmptyForNoMatches() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            // when
            List<ArchType> entities = registry.ofKind(ArchKind.ENTITY).toList();

            // then
            assertThat(entities).isEmpty();
        }
    }

    @Nested
    @DisplayName("size()")
    class SizeTests {

        @Test
        @DisplayName("should return correct size")
        void shouldReturnCorrectSize() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            ValueObject money = createValueObject(MONEY_ID);
            UnclassifiedType utils = createUnclassified(UTILS_ID);

            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(money).add(utils).build();

            // then
            assertThat(registry.size()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("should return immutable stream results from toList()")
        void shouldReturnImmutableStreamResults() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID);
            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            // when
            List<ArchType> types = registry.all().toList();

            // then - toList() returns an unmodifiable list since Java 16
            assertThatThrownBy(() -> types.add(createAggregate(ITEM_ID)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
