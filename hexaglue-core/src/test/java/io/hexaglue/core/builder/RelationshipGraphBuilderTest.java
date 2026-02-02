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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.graph.RelationType;
import io.hexaglue.arch.model.graph.RelationshipGraph;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RelationshipGraphBuilder}.
 *
 * @since 5.0.0
 */
@DisplayName("RelationshipGraphBuilder")
class RelationshipGraphBuilderTest {

    // Type IDs for test types
    private static final TypeId ORDER_ID = TypeId.of("com.example.order.Order");
    private static final TypeId ORDER_LINE_ID = TypeId.of("com.example.order.OrderLine");
    private static final TypeId MONEY_ID = TypeId.of("com.example.order.Money");
    private static final TypeId ORDER_CREATED_ID = TypeId.of("com.example.order.OrderCreated");
    private static final TypeId ORDER_REPOSITORY_ID = TypeId.of("com.example.order.OrderRepository");
    private static final TypeId ORDER_SERVICE_ID = TypeId.of("com.example.order.OrderService");

    private RelationshipGraphBuilder builder;
    private TypeStructure classStructure;
    private TypeStructure interfaceStructure;
    private ClassificationTrace aggTrace;
    private ClassificationTrace entityTrace;
    private ClassificationTrace voTrace;
    private ClassificationTrace eventTrace;
    private ClassificationTrace repoTrace;
    private ClassificationTrace serviceTrace;

    @BeforeEach
    void setUp() {
        builder = new RelationshipGraphBuilder();
        classStructure = TypeStructure.builder(TypeNature.CLASS).build();
        interfaceStructure = TypeStructure.builder(TypeNature.INTERFACE).build();

        aggTrace = ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test", "Test");
        entityTrace = ClassificationTrace.highConfidence(ElementKind.ENTITY, "test", "Test");
        voTrace = ClassificationTrace.highConfidence(ElementKind.VALUE_OBJECT, "test", "Test");
        eventTrace = ClassificationTrace.highConfidence(ElementKind.DOMAIN_EVENT, "test", "Test");
        repoTrace = ClassificationTrace.highConfidence(ElementKind.DRIVEN_PORT, "test", "Test");
        serviceTrace = ClassificationTrace.highConfidence(ElementKind.DOMAIN_SERVICE, "test", "Test");
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should reject null registry")
        void shouldRejectNullRegistry() {
            assertThatThrownBy(() -> builder.build(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("registry");
        }

        @Test
        @DisplayName("should return empty graph for empty registry")
        void shouldReturnEmptyGraphForEmptyRegistry() {
            // given
            TypeRegistry registry = TypeRegistry.builder().build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("CONTAINS Relationships")
    class ContainsRelationships {

        @Test
        @DisplayName("should create CONTAINS for aggregate -> entity")
        void shouldCreateContainsForAggregateToEntity() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                    .entities(List.of(TypeRef.of(ORDER_LINE_ID.qualifiedName())))
                    .build();

            Entity orderLine = Entity.of(ORDER_LINE_ID, classStructure, entityTrace);

            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(orderLine).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_ID, ORDER_LINE_ID, RelationType.CONTAINS))
                    .isTrue();
        }

        @Test
        @DisplayName("should create CONTAINS for aggregate -> value object")
        void shouldCreateContainsForAggregateToValueObject() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                    .valueObjects(List.of(TypeRef.of(MONEY_ID.qualifiedName())))
                    .build();

            ValueObject money = ValueObject.of(MONEY_ID, classStructure, voTrace);

            TypeRegistry registry = TypeRegistry.builder().add(order).add(money).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_ID, MONEY_ID, RelationType.CONTAINS))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("EMITS Relationships")
    class EmitsRelationships {

        @Test
        @DisplayName("should create EMITS for aggregate -> domain event")
        void shouldCreateEmitsForAggregateToDomainEvent() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                    .domainEvents(List.of(TypeRef.of(ORDER_CREATED_ID.qualifiedName())))
                    .build();

            DomainEvent orderCreated = DomainEvent.of(ORDER_CREATED_ID, classStructure, eventTrace);

            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(orderCreated).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_ID, ORDER_CREATED_ID, RelationType.EMITS))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PERSISTS Relationships")
    class PersistsRelationships {

        @Test
        @DisplayName("should create PERSISTS for repository -> aggregate")
        void shouldCreatePersistsForRepositoryToAggregate() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                    .build();

            DrivenPort orderRepository = DrivenPort.repository(
                    ORDER_REPOSITORY_ID, interfaceStructure, repoTrace, TypeRef.of(ORDER_ID.qualifiedName()));

            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(orderRepository).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_REPOSITORY_ID, ORDER_ID, RelationType.PERSISTS))
                    .isTrue();
        }

        @Test
        @DisplayName("should not create PERSISTS for non-repository driven port")
        void shouldNotCreatePersistsForNonRepository() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                    .build();

            DrivenPort gateway =
                    DrivenPort.of(ORDER_REPOSITORY_ID, interfaceStructure, repoTrace, DrivenPortType.GATEWAY);

            TypeRegistry registry =
                    TypeRegistry.builder().add(order).add(gateway).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_REPOSITORY_ID, ORDER_ID, RelationType.PERSISTS))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("DEPENDS_ON Relationships")
    class DependsOnRelationships {

        @Test
        @DisplayName("should create DEPENDS_ON for domain service -> injected port")
        void shouldCreateDependsOnForServiceToInjectedPort() {
            // given
            DrivenPort orderRepository =
                    DrivenPort.of(ORDER_REPOSITORY_ID, interfaceStructure, repoTrace, DrivenPortType.REPOSITORY);

            DomainService orderService = DomainService.of(
                    ORDER_SERVICE_ID,
                    classStructure,
                    serviceTrace,
                    List.of(TypeRef.of(ORDER_REPOSITORY_ID.qualifiedName())),
                    List.of());

            TypeRegistry registry = TypeRegistry.builder()
                    .add(orderService)
                    .add(orderRepository)
                    .build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_SERVICE_ID, ORDER_REPOSITORY_ID, RelationType.DEPENDS_ON))
                    .isTrue();
        }

        @Test
        @DisplayName("should create DEPENDS_ON for field references to classified types")
        void shouldCreateDependsOnForFieldReferences() {
            // given
            Field moneyField = Field.builder("amount", TypeRef.of(MONEY_ID.qualifiedName()))
                    .build();
            TypeStructure structureWithField = TypeStructure.builder(TypeNature.CLASS)
                    .fields(List.of(moneyField))
                    .build();

            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, structureWithField, aggTrace, idField)
                    .build();

            ValueObject money = ValueObject.of(MONEY_ID, classStructure, voTrace);

            TypeRegistry registry = TypeRegistry.builder().add(order).add(money).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_ID, MONEY_ID, RelationType.DEPENDS_ON))
                    .isTrue();
        }

        @Test
        @DisplayName("should not create DEPENDS_ON for unclassified field types")
        void shouldNotCreateDependsOnForUnclassifiedTypes() {
            // given
            Field stringField =
                    Field.builder("name", TypeRef.of("java.lang.String")).build();
            TypeStructure structureWithField = TypeStructure.builder(TypeNature.CLASS)
                    .fields(List.of(stringField))
                    .build();

            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, structureWithField, aggTrace, idField)
                    .build();

            TypeRegistry registry = TypeRegistry.builder().add(order).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            TypeId stringId = TypeId.of("java.lang.String");
            assertThat(graph.hasRelation(ORDER_ID, stringId, RelationType.DEPENDS_ON))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("IMPLEMENTS Relationships")
    class ImplementsRelationships {

        @Test
        @DisplayName("should create IMPLEMENTS for type -> interface")
        void shouldCreateImplementsForTypeToInterface() {
            // given
            TypeStructure structureWithInterface = TypeStructure.builder(TypeNature.CLASS)
                    .interfaces(List.of(TypeRef.of(ORDER_REPOSITORY_ID.qualifiedName())))
                    .build();

            // Create a service that implements the repository interface
            DomainService service =
                    DomainService.of(ORDER_SERVICE_ID, structureWithInterface, serviceTrace, List.of(), List.of());

            DrivenPort repository =
                    DrivenPort.of(ORDER_REPOSITORY_ID, interfaceStructure, repoTrace, DrivenPortType.REPOSITORY);

            TypeRegistry registry =
                    TypeRegistry.builder().add(service).add(repository).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_SERVICE_ID, ORDER_REPOSITORY_ID, RelationType.IMPLEMENTS))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("OUT_OF_SCOPE Filtering")
    class OutOfScopeFiltering {

        @Test
        @DisplayName("should keep IMPLEMENTS for OUT_OF_SCOPE types (adapter detection)")
        void shouldKeepImplementsForOutOfScopeTypes() {
            // given - a generated adapter (OUT_OF_SCOPE) that implements a port interface
            DrivenPort repository =
                    DrivenPort.of(ORDER_REPOSITORY_ID, interfaceStructure, repoTrace, DrivenPortType.REPOSITORY);

            TypeId adapterId = TypeId.of("com.example.adapter.persistence.OrderRepositoryAdapter");
            ClassificationTrace unclTrace =
                    ClassificationTrace.highConfidence(ElementKind.UNCLASSIFIED, "test", "Generated adapter");
            TypeStructure adapterStructure = TypeStructure.builder(TypeNature.CLASS)
                    .interfaces(List.of(TypeRef.of(ORDER_REPOSITORY_ID.qualifiedName())))
                    .build();
            UnclassifiedType adapter = UnclassifiedType.of(
                    adapterId, adapterStructure, unclTrace, UnclassifiedType.UnclassifiedCategory.OUT_OF_SCOPE);

            TypeRegistry registry =
                    TypeRegistry.builder().add(repository).add(adapter).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then - OUT_OF_SCOPE type should produce IMPLEMENTS relationship for adapter detection
            assertThat(graph.hasRelation(adapterId, ORDER_REPOSITORY_ID, RelationType.IMPLEMENTS))
                    .isTrue();
        }

        @Test
        @DisplayName("should skip field compositions for OUT_OF_SCOPE types")
        void shouldSkipFieldCompositionsForOutOfScopeTypes() {
            // given - a generated JPA entity (OUT_OF_SCOPE) with fields referencing domain types
            ValueObject money = ValueObject.of(MONEY_ID, classStructure, voTrace);

            TypeId jpaEntityId = TypeId.of("com.example.adapter.persistence.OrderJpaEntity");
            ClassificationTrace unclTrace =
                    ClassificationTrace.highConfidence(ElementKind.UNCLASSIFIED, "test", "Generated entity");
            Field moneyField = Field.builder("totalAmount", TypeRef.of(MONEY_ID.qualifiedName()))
                    .build();
            TypeStructure jpaStructure = TypeStructure.builder(TypeNature.CLASS)
                    .fields(List.of(moneyField))
                    .build();
            UnclassifiedType jpaEntity = UnclassifiedType.of(
                    jpaEntityId, jpaStructure, unclTrace, UnclassifiedType.UnclassifiedCategory.OUT_OF_SCOPE);

            TypeRegistry registry =
                    TypeRegistry.builder().add(money).add(jpaEntity).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then - OUT_OF_SCOPE type should NOT produce field composition relationships
            assertThat(graph.from(jpaEntityId).count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should still include non-OUT_OF_SCOPE unclassified types")
        void shouldIncludeNonOutOfScopeUnclassified() {
            // given - an AMBIGUOUS unclassified type that has a field referencing a known type
            ValueObject money = ValueObject.of(MONEY_ID, classStructure, voTrace);

            TypeId utilId = TypeId.of("com.example.order.PriceCalculator");
            ClassificationTrace unclTrace =
                    ClassificationTrace.highConfidence(ElementKind.UNCLASSIFIED, "test", "Ambiguous type");
            Field moneyField = Field.builder("amount", TypeRef.of(MONEY_ID.qualifiedName()))
                    .build();
            TypeStructure utilStructure = TypeStructure.builder(TypeNature.CLASS)
                    .fields(List.of(moneyField))
                    .build();
            UnclassifiedType util = UnclassifiedType.of(
                    utilId, utilStructure, unclTrace, UnclassifiedType.UnclassifiedCategory.AMBIGUOUS);

            TypeRegistry registry = TypeRegistry.builder().add(money).add(util).build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then - AMBIGUOUS type should still produce relationships
            assertThat(graph.hasRelation(utilId, MONEY_ID, RelationType.DEPENDS_ON))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenarios {

        @Test
        @DisplayName("should build complete graph for typical aggregate structure")
        void shouldBuildCompleteGraphForTypicalAggregate() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                    .entities(List.of(TypeRef.of(ORDER_LINE_ID.qualifiedName())))
                    .valueObjects(List.of(TypeRef.of(MONEY_ID.qualifiedName())))
                    .domainEvents(List.of(TypeRef.of(ORDER_CREATED_ID.qualifiedName())))
                    .build();

            Entity orderLine = Entity.of(ORDER_LINE_ID, classStructure, entityTrace);
            ValueObject money = ValueObject.of(MONEY_ID, classStructure, voTrace);
            DomainEvent orderCreated = DomainEvent.of(ORDER_CREATED_ID, classStructure, eventTrace);
            DrivenPort orderRepository = DrivenPort.repository(
                    ORDER_REPOSITORY_ID, interfaceStructure, repoTrace, TypeRef.of(ORDER_ID.qualifiedName()));

            TypeRegistry registry = TypeRegistry.builder()
                    .add(order)
                    .add(orderLine)
                    .add(money)
                    .add(orderCreated)
                    .add(orderRepository)
                    .build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            assertThat(graph.hasRelation(ORDER_ID, ORDER_LINE_ID, RelationType.CONTAINS))
                    .isTrue();
            assertThat(graph.hasRelation(ORDER_ID, MONEY_ID, RelationType.CONTAINS))
                    .isTrue();
            assertThat(graph.hasRelation(ORDER_ID, ORDER_CREATED_ID, RelationType.EMITS))
                    .isTrue();
            assertThat(graph.hasRelation(ORDER_REPOSITORY_ID, ORDER_ID, RelationType.PERSISTS))
                    .isTrue();

            // Verify counts
            assertThat(graph.from(ORDER_ID).count()).isGreaterThanOrEqualTo(3);
            assertThat(graph.to(ORDER_ID).count()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("should navigate graph from aggregate to all related types")
        void shouldNavigateGraphFromAggregate() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

            AggregateRoot order = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                    .entities(List.of(TypeRef.of(ORDER_LINE_ID.qualifiedName())))
                    .domainEvents(List.of(TypeRef.of(ORDER_CREATED_ID.qualifiedName())))
                    .build();

            Entity orderLine = Entity.of(ORDER_LINE_ID, classStructure, entityTrace);
            DomainEvent orderCreated = DomainEvent.of(ORDER_CREATED_ID, classStructure, eventTrace);

            TypeRegistry registry = TypeRegistry.builder()
                    .add(order)
                    .add(orderLine)
                    .add(orderCreated)
                    .build();

            // when
            RelationshipGraph graph = builder.build(registry);

            // then
            var relatedTypes = graph.relatedTo(ORDER_ID);
            assertThat(relatedTypes).contains(ORDER_LINE_ID, ORDER_CREATED_ID);
        }
    }
}
