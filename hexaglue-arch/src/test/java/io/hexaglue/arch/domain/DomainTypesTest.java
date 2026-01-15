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

package io.hexaglue.arch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.Cardinality;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.arch.ElementRegistry;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Domain Types")
class DomainTypesTest {

    private static final String PKG = "com.example.domain";

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @Nested
    @DisplayName("ValueObject")
    class ValueObjectTest {

        @Test
        @DisplayName("should create value object with fields")
        void shouldCreateWithFields() {
            // when
            ValueObject vo = new ValueObject(
                    ElementId.of(PKG + ".Money"),
                    List.of("amount", "currency"),
                    null,
                    highConfidence(ElementKind.VALUE_OBJECT));

            // then
            assertThat(vo.id().qualifiedName()).isEqualTo(PKG + ".Money");
            assertThat(vo.kind()).isEqualTo(ElementKind.VALUE_OBJECT);
            assertThat(vo.componentFields()).containsExactly("amount", "currency");
            assertThat(vo.simpleName()).isEqualTo("Money");
        }

        @Test
        @DisplayName("should use factory method")
        void shouldUseFactory() {
            // when
            ValueObject vo =
                    ValueObject.of(PKG + ".Money", List.of("amount"), highConfidence(ElementKind.VALUE_OBJECT));

            // then
            assertThat(vo.id().qualifiedName()).isEqualTo(PKG + ".Money");
            assertThat(vo.syntax()).isNull();
        }

        @Test
        @DisplayName("should reject null fields")
        void shouldRejectNullFields() {
            assertThatThrownBy(() -> new ValueObject(
                            ElementId.of(PKG + ".Money"), null, null, highConfidence(ElementKind.VALUE_OBJECT)))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should return immutable field list")
        void shouldReturnImmutableFieldList() {
            // given
            List<String> fields = new java.util.ArrayList<>(List.of("amount"));
            ValueObject vo = new ValueObject(
                    ElementId.of(PKG + ".Money"), fields, null, highConfidence(ElementKind.VALUE_OBJECT));

            // when/then
            assertThatThrownBy(() -> vo.componentFields().add("currency"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Identifier")
    class IdentifierTest {

        @Test
        @DisplayName("should create identifier")
        void shouldCreate() {
            // when
            Identifier id = new Identifier(
                    ElementId.of(PKG + ".OrderId"),
                    TypeRef.of("java.util.UUID"),
                    PKG + ".Order",
                    null,
                    highConfidence(ElementKind.IDENTIFIER));

            // then
            assertThat(id.kind()).isEqualTo(ElementKind.IDENTIFIER);
            assertThat(id.hasWrappedType()).isTrue();
            assertThat(id.hasIdentifiesType()).isTrue();
            assertThat(id.wrappedType().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should create without optional fields")
        void shouldCreateWithoutOptionalFields() {
            // when
            Identifier id = Identifier.of(PKG + ".OrderId", highConfidence(ElementKind.IDENTIFIER));

            // then
            assertThat(id.hasWrappedType()).isFalse();
            assertThat(id.hasIdentifiesType()).isFalse();
        }
    }

    @Nested
    @DisplayName("DomainEvent")
    class DomainEventTest {

        @Test
        @DisplayName("should create domain event")
        void shouldCreate() {
            // when
            DomainEvent event = new DomainEvent(
                    ElementId.of(PKG + ".OrderPlaced"),
                    PKG + ".Order",
                    List.of("orderId", "occurredAt"),
                    null,
                    highConfidence(ElementKind.DOMAIN_EVENT));

            // then
            assertThat(event.kind()).isEqualTo(ElementKind.DOMAIN_EVENT);
            assertThat(event.hasPublisher()).isTrue();
            assertThat(event.eventFields()).containsExactly("orderId", "occurredAt");
        }

        @Test
        @DisplayName("should create without publisher")
        void shouldCreateWithoutPublisher() {
            // when
            DomainEvent event = DomainEvent.of(PKG + ".SomeEvent", highConfidence(ElementKind.DOMAIN_EVENT));

            // then
            assertThat(event.hasPublisher()).isFalse();
        }
    }

    @Nested
    @DisplayName("DomainService")
    class DomainServiceTest {

        @Test
        @DisplayName("should create domain service")
        void shouldCreate() {
            // when
            DomainService service = new DomainService(
                    ElementId.of(PKG + ".PricingService"),
                    List.of("calculatePrice", "applyDiscount"),
                    null,
                    highConfidence(ElementKind.DOMAIN_SERVICE));

            // then
            assertThat(service.kind()).isEqualTo(ElementKind.DOMAIN_SERVICE);
            assertThat(service.operations()).containsExactly("calculatePrice", "applyDiscount");
        }
    }

    @Nested
    @DisplayName("DomainEntity")
    class DomainEntityTest {

        @Test
        @DisplayName("should create entity")
        void shouldCreateEntity() {
            // when
            DomainEntity entity = DomainEntity.entity(PKG + ".OrderLine", highConfidence(ElementKind.ENTITY));

            // then
            assertThat(entity.kind()).isEqualTo(ElementKind.ENTITY);
            assertThat(entity.isAggregateRoot()).isFalse();
        }

        @Test
        @DisplayName("should create aggregate root")
        void shouldCreateAggregateRoot() {
            // when
            DomainEntity root = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));

            // then
            assertThat(root.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(root.isAggregateRoot()).isTrue();
        }

        @Test
        @DisplayName("should reject invalid kind")
        void shouldRejectInvalidKind() {
            assertThatThrownBy(() -> new DomainEntity(
                            ElementId.of(PKG + ".Order"),
                            ElementKind.VALUE_OBJECT,
                            null,
                            null,
                            Optional.empty(),
                            List.of(),
                            null,
                            highConfidence(ElementKind.VALUE_OBJECT)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ENTITY or AGGREGATE_ROOT");
        }

        @Test
        @DisplayName("should track identity field")
        void shouldTrackIdentityField() {
            // when
            DomainEntity entity = new DomainEntity(
                    ElementId.of(PKG + ".Order"),
                    ElementKind.AGGREGATE_ROOT,
                    "id",
                    TypeRef.of(PKG + ".OrderId"),
                    Optional.empty(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.AGGREGATE_ROOT));

            // then
            assertThat(entity.hasIdentity()).isTrue();
            assertThat(entity.identityField()).isEqualTo("id");
        }
    }

    @Nested
    @DisplayName("Aggregate")
    class AggregateTest {

        @Test
        @DisplayName("should create aggregate with root")
        void shouldCreateWithRoot() {
            // given
            ElementRef<DomainEntity> rootRef = ElementRef.of(ElementId.of(PKG + ".Order"), DomainEntity.class);

            // when
            Aggregate agg = Aggregate.of(PKG + ".OrderAggregate", rootRef, highConfidence(ElementKind.AGGREGATE));

            // then
            assertThat(agg.kind()).isEqualTo(ElementKind.AGGREGATE);
            assertThat(agg.root()).isEqualTo(rootRef);
            assertThat(agg.internalEntities()).isEmpty();
            assertThat(agg.valueObjects()).isEmpty();
            assertThat(agg.publishedEvents()).isEmpty();
            assertThat(agg.externalReferences()).isEmpty();
        }

        @Test
        @DisplayName("should track published events")
        void shouldTrackPublishedEvents() {
            // given
            ElementRef<DomainEntity> rootRef = ElementRef.of(ElementId.of(PKG + ".Order"), DomainEntity.class);
            ElementRef<DomainEvent> eventRef = ElementRef.of(ElementId.of(PKG + ".OrderPlaced"), DomainEvent.class);

            // when
            Aggregate agg = new Aggregate(
                    ElementId.of(PKG + ".OrderAggregate"),
                    rootRef,
                    List.of(),
                    List.of(),
                    List.of(eventRef),
                    List.of(),
                    highConfidence(ElementKind.AGGREGATE));

            // then
            assertThat(agg.publishesEvents()).isTrue();
            assertThat(agg.publishedEvents()).hasSize(1);
        }

        @Test
        @DisplayName("should resolve root from registry")
        void shouldResolveRoot() {
            // given
            DomainEntity root = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));
            ElementRef<DomainEntity> rootRef = ElementRef.of(root.id(), DomainEntity.class);
            Aggregate agg = Aggregate.of(PKG + ".OrderAggregate", rootRef, highConfidence(ElementKind.AGGREGATE));

            ElementRegistry registry =
                    ElementRegistry.builder().add(root).add(agg).build();

            // when
            DomainEntity resolved = agg.resolveRoot(registry);

            // then
            assertThat(resolved).isEqualTo(root);
        }
    }

    @Nested
    @DisplayName("AggregateReference")
    class AggregateReferenceTest {

        @Test
        @DisplayName("should create aggregate reference")
        void shouldCreate() {
            // given
            ElementRef<Aggregate> targetRef = ElementRef.of(ElementId.of(PKG + ".CustomerAggregate"), Aggregate.class);

            // when
            AggregateReference ref =
                    new AggregateReference("customerId", targetRef, TypeRef.of(PKG + ".CustomerId"), Cardinality.ONE);

            // then
            assertThat(ref.propertyName()).isEqualTo("customerId");
            assertThat(ref.isRequired()).isTrue();
            assertThat(ref.isMany()).isFalse();
        }

        @Test
        @DisplayName("should track many cardinality")
        void shouldTrackManyCardinality() {
            // given
            ElementRef<Aggregate> targetRef = ElementRef.of(ElementId.of(PKG + ".ProductAggregate"), Aggregate.class);

            // when
            AggregateReference ref = new AggregateReference(
                    "productIds", targetRef, TypeRef.of(PKG + ".ProductId"), Cardinality.ZERO_OR_MANY);

            // then
            assertThat(ref.isRequired()).isFalse();
            assertThat(ref.isMany()).isTrue();
        }

        @Test
        @DisplayName("should reject blank property name")
        void shouldRejectBlankPropertyName() {
            ElementRef<Aggregate> targetRef = ElementRef.of(ElementId.of(PKG + ".CustomerAggregate"), Aggregate.class);

            assertThatThrownBy(() ->
                            new AggregateReference("  ", targetRef, TypeRef.of(PKG + ".CustomerId"), Cardinality.ONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }
    }

    @Nested
    @DisplayName("Cardinality")
    class CardinalityTest {

        @Test
        @DisplayName("should identify required cardinalities")
        void shouldIdentifyRequired() {
            assertThat(Cardinality.ONE.isRequired()).isTrue();
            assertThat(Cardinality.ONE_OR_MANY.isRequired()).isTrue();
            assertThat(Cardinality.ZERO_OR_ONE.isRequired()).isFalse();
            assertThat(Cardinality.ZERO_OR_MANY.isRequired()).isFalse();
        }

        @Test
        @DisplayName("should identify many cardinalities")
        void shouldIdentifyMany() {
            assertThat(Cardinality.ONE.isMany()).isFalse();
            assertThat(Cardinality.ZERO_OR_ONE.isMany()).isFalse();
            assertThat(Cardinality.ZERO_OR_MANY.isMany()).isTrue();
            assertThat(Cardinality.ONE_OR_MANY.isMany()).isTrue();
        }
    }
}
