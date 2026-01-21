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

package io.hexaglue.arch.model.index;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainIndex}.
 *
 * @since 4.1.0
 */
@DisplayName("DomainIndex")
class DomainIndexTest {

    private static final TypeId ORDER_ID = TypeId.of("com.example.Order");
    private static final TypeId ITEM_ID = TypeId.of("com.example.OrderLine");
    private static final TypeId MONEY_ID = TypeId.of("com.example.Money");
    private static final TypeId ORDER_ID_TYPE = TypeId.of("com.example.OrderId");
    private static final TypeId ORDER_PLACED_ID = TypeId.of("com.example.OrderPlaced");
    private static final TypeId PRICING_SERVICE_ID = TypeId.of("com.example.PricingService");

    private TypeStructure classStructure;
    private TypeStructure recordStructure;
    private Field idField;

    @BeforeEach
    void setUp() {
        classStructure = TypeStructure.builder(TypeNature.CLASS).build();
        recordStructure = TypeStructure.builder(TypeNature.RECORD).build();
        idField = Field.of("id", TypeRef.of("java.util.UUID"));
    }

    private AggregateRoot createAggregate(TypeId id, List<TypeRef> entities, List<TypeRef> valueObjects) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test", "Test");
        return AggregateRoot.builder(id, classStructure, trace, idField)
                .entities(entities)
                .valueObjects(valueObjects)
                .build();
    }

    private Entity createEntity(TypeId id) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.ENTITY, "test", "Test");
        return Entity.of(id, classStructure, trace, idField);
    }

    private ValueObject createValueObject(TypeId id) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.VALUE_OBJECT, "test", "Test");
        return new ValueObject(id, recordStructure, trace);
    }

    private Identifier createIdentifier(TypeId id) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.IDENTIFIER, "test", "Test");
        return Identifier.of(id, recordStructure, trace, TypeRef.of("java.util.UUID"));
    }

    private DomainEvent createDomainEvent(TypeId id) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.DOMAIN_EVENT, "test", "Test");
        return DomainEvent.of(id, recordStructure, trace);
    }

    private DomainService createDomainService(TypeId id) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.DOMAIN_SERVICE, "test", "Test");
        TypeStructure interfaceStructure =
                TypeStructure.builder(TypeNature.INTERFACE).build();
        return DomainService.of(id, interfaceStructure, trace);
    }

    @Nested
    @DisplayName("from(TypeRegistry)")
    class FromTypeRegistry {

        @Test
        @DisplayName("should create from empty registry")
        void shouldCreateFromEmptyRegistry() {
            // given
            TypeRegistry registry = TypeRegistry.builder().build();

            // when
            DomainIndex index = DomainIndex.from(registry);

            // then
            assertThat(index.aggregateRoots().count()).isEqualTo(0);
            assertThat(index.entities().count()).isEqualTo(0);
            assertThat(index.valueObjects().count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should create from registry with domain types")
        void shouldCreateFromRegistryWithDomainTypes() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID, List.of(), List.of());
            Entity item = createEntity(ITEM_ID);
            ValueObject money = createValueObject(MONEY_ID);

            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(item).add(money).build();

            // when
            DomainIndex index = DomainIndex.from(registry);

            // then
            assertThat(index.aggregateRoots().count()).isEqualTo(1);
            assertThat(index.entities().count()).isEqualTo(1);
            assertThat(index.valueObjects().count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("aggregateRoots()")
    class AggregateRoots {

        @Test
        @DisplayName("should return all aggregate roots")
        void shouldReturnAllAggregateRoots() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID, List.of(), List.of());
            AggregateRoot item = createAggregate(ITEM_ID, List.of(), List.of());
            ValueObject money = createValueObject(MONEY_ID);

            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(item).add(money).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<AggregateRoot> aggregates = index.aggregateRoots().toList();

            // then
            assertThat(aggregates).containsExactlyInAnyOrder(order, item);
        }
    }

    @Nested
    @DisplayName("entities()")
    class Entities {

        @Test
        @DisplayName("should return all entities")
        void shouldReturnAllEntities() {
            // given
            Entity item = createEntity(ITEM_ID);
            AggregateRoot order = createAggregate(ORDER_ID, List.of(), List.of());

            TypeRegistry registry = TypeRegistry.builder().add(item).add(order).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<Entity> entities = index.entities().toList();

            // then
            assertThat(entities).containsExactly(item);
        }
    }

    @Nested
    @DisplayName("valueObjects()")
    class ValueObjects {

        @Test
        @DisplayName("should return all value objects")
        void shouldReturnAllValueObjects() {
            // given
            ValueObject money = createValueObject(MONEY_ID);
            AggregateRoot order = createAggregate(ORDER_ID, List.of(), List.of());

            TypeRegistry registry = TypeRegistry.builder().add(money).add(order).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<ValueObject> valueObjects = index.valueObjects().toList();

            // then
            assertThat(valueObjects).containsExactly(money);
        }
    }

    @Nested
    @DisplayName("identifiers()")
    class Identifiers {

        @Test
        @DisplayName("should return all identifiers")
        void shouldReturnAllIdentifiers() {
            // given
            Identifier orderId = createIdentifier(ORDER_ID_TYPE);

            TypeRegistry registry = TypeRegistry.builder().add(orderId).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<Identifier> identifiers = index.identifiers().toList();

            // then
            assertThat(identifiers).containsExactly(orderId);
        }
    }

    @Nested
    @DisplayName("domainEvents()")
    class DomainEvents {

        @Test
        @DisplayName("should return all domain events")
        void shouldReturnAllDomainEvents() {
            // given
            DomainEvent orderPlaced = createDomainEvent(ORDER_PLACED_ID);

            TypeRegistry registry = TypeRegistry.builder().add(orderPlaced).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<DomainEvent> events = index.domainEvents().toList();

            // then
            assertThat(events).containsExactly(orderPlaced);
        }
    }

    @Nested
    @DisplayName("domainServices()")
    class DomainServices {

        @Test
        @DisplayName("should return all domain services")
        void shouldReturnAllDomainServices() {
            // given
            DomainService pricingService = createDomainService(PRICING_SERVICE_ID);

            TypeRegistry registry = TypeRegistry.builder().add(pricingService).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<DomainService> services = index.domainServices().toList();

            // then
            assertThat(services).containsExactly(pricingService);
        }
    }

    @Nested
    @DisplayName("aggregateRoot(TypeId)")
    class AggregateRootById {

        @Test
        @DisplayName("should return aggregate root by id")
        void shouldReturnAggregateRootById() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID, List.of(), List.of());

            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            Optional<AggregateRoot> result = index.aggregateRoot(ORDER_ID);

            // then
            assertThat(result).contains(order);
        }

        @Test
        @DisplayName("should return empty for non-existent id")
        void shouldReturnEmptyForNonExistentId() {
            // given
            TypeRegistry registry = TypeRegistry.builder().build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            Optional<AggregateRoot> result = index.aggregateRoot(ORDER_ID);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for wrong type")
        void shouldReturnEmptyForWrongType() {
            // given
            Entity item = createEntity(ITEM_ID);

            TypeRegistry registry = TypeRegistry.builder().add(item).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            Optional<AggregateRoot> result = index.aggregateRoot(ITEM_ID);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("entitiesOf(AggregateRoot)")
    class EntitiesOfAggregate {

        @Test
        @DisplayName("should return entities of aggregate")
        void shouldReturnEntitiesOfAggregate() {
            // given
            Entity item = createEntity(ITEM_ID);
            AggregateRoot order = createAggregate(ORDER_ID, List.of(TypeRef.of(ITEM_ID.qualifiedName())), List.of());

            TypeRegistry registry = TypeRegistry.builder().add(order).add(item).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<Entity> entities = index.entitiesOf(order);

            // then
            assertThat(entities).containsExactly(item);
        }

        @Test
        @DisplayName("should return empty for aggregate without entities")
        void shouldReturnEmptyForNoEntities() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID, List.of(), List.of());

            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<Entity> entities = index.entitiesOf(order);

            // then
            assertThat(entities).isEmpty();
        }
    }

    @Nested
    @DisplayName("valueObjectsOf(AggregateRoot)")
    class ValueObjectsOfAggregate {

        @Test
        @DisplayName("should return value objects of aggregate")
        void shouldReturnValueObjectsOfAggregate() {
            // given
            ValueObject money = createValueObject(MONEY_ID);
            AggregateRoot order = createAggregate(ORDER_ID, List.of(), List.of(TypeRef.of(MONEY_ID.qualifiedName())));

            TypeRegistry registry = TypeRegistry.builder().add(order).add(money).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<ValueObject> valueObjects = index.valueObjectsOf(order);

            // then
            assertThat(valueObjects).containsExactly(money);
        }

        @Test
        @DisplayName("should return empty for aggregate without value objects")
        void shouldReturnEmptyForNoValueObjects() {
            // given
            AggregateRoot order = createAggregate(ORDER_ID, List.of(), List.of());

            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            DomainIndex index = DomainIndex.from(registry);

            // when
            List<ValueObject> valueObjects = index.valueObjectsOf(order);

            // then
            assertThat(valueObjects).isEmpty();
        }
    }
}
