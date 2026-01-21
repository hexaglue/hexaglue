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
import io.hexaglue.arch.ElementKind;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainEvent}.
 *
 * @since 4.1.0
 */
@DisplayName("DomainEvent")
class DomainEventTest {

    private static final TypeId ID = TypeId.of("com.example.OrderCreated");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.RECORD).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.DOMAIN_EVENT, "explicit-domain-event", "Has DomainEvent annotation");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            DomainEvent event = DomainEvent.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(event.id()).isEqualTo(ID);
            assertThat(event.structure()).isEqualTo(STRUCTURE);
            assertThat(event.classification()).isEqualTo(TRACE);
        }

        @Test
        @DisplayName("should return DOMAIN_EVENT kind")
        void shouldReturnDomainEventKind() {
            // when
            DomainEvent event = DomainEvent.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(event.kind()).isEqualTo(ArchKind.DOMAIN_EVENT);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> DomainEvent.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> DomainEvent.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> DomainEvent.of(ID, STRUCTURE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement DomainType interface")
        void shouldImplementDomainType() {
            // given
            DomainEvent event = DomainEvent.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(event).isInstanceOf(DomainType.class);
            assertThat(event).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            DomainEvent event = DomainEvent.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(event.qualifiedName()).isEqualTo("com.example.OrderCreated");
            assertThat(event.simpleName()).isEqualTo("OrderCreated");
            assertThat(event.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Event Metadata")
    class EventMetadata {

        private static final TypeRef UUID_TYPE = new TypeRef("java.util.UUID", "UUID", List.of(), false, false, 0);
        private static final TypeRef INSTANT_TYPE =
                new TypeRef("java.time.Instant", "Instant", List.of(), false, false, 0);
        private static final TypeRef ORDER_TYPE = new TypeRef("com.example.Order", "Order", List.of(), false, false, 0);

        @Test
        @DisplayName("should create event without metadata fields")
        void shouldCreateEventWithoutMetadataFields() {
            // when
            DomainEvent event = DomainEvent.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(event.aggregateIdField()).isEmpty();
            assertThat(event.timestampField()).isEmpty();
            assertThat(event.sourceAggregate()).isEmpty();
            assertThat(event.hasAggregateIdField()).isFalse();
            assertThat(event.hasTimestampField()).isFalse();
            assertThat(event.hasSourceAggregate()).isFalse();
        }

        @Test
        @DisplayName("should create event with aggregate id field")
        void shouldCreateEventWithAggregateIdField() {
            // given
            Field aggregateIdField = Field.builder("orderId", UUID_TYPE).build();

            // when
            DomainEvent event = DomainEvent.of(
                    ID, STRUCTURE, TRACE, Optional.of(aggregateIdField), Optional.empty(), Optional.empty());

            // then
            assertThat(event.aggregateIdField()).contains(aggregateIdField);
            assertThat(event.hasAggregateIdField()).isTrue();
        }

        @Test
        @DisplayName("should create event with timestamp field")
        void shouldCreateEventWithTimestampField() {
            // given
            Field timestampField = Field.builder("occurredAt", INSTANT_TYPE).build();

            // when
            DomainEvent event = DomainEvent.of(
                    ID, STRUCTURE, TRACE, Optional.empty(), Optional.of(timestampField), Optional.empty());

            // then
            assertThat(event.timestampField()).contains(timestampField);
            assertThat(event.hasTimestampField()).isTrue();
        }

        @Test
        @DisplayName("should create event with source aggregate")
        void shouldCreateEventWithSourceAggregate() {
            // when
            DomainEvent event =
                    DomainEvent.of(ID, STRUCTURE, TRACE, Optional.empty(), Optional.empty(), Optional.of(ORDER_TYPE));

            // then
            assertThat(event.sourceAggregate()).contains(ORDER_TYPE);
            assertThat(event.hasSourceAggregate()).isTrue();
        }

        @Test
        @DisplayName("should create event with all metadata fields")
        void shouldCreateEventWithAllMetadataFields() {
            // given
            Field aggregateIdField = Field.builder("orderId", UUID_TYPE).build();
            Field timestampField = Field.builder("occurredAt", INSTANT_TYPE).build();

            // when
            DomainEvent event = DomainEvent.of(
                    ID,
                    STRUCTURE,
                    TRACE,
                    Optional.of(aggregateIdField),
                    Optional.of(timestampField),
                    Optional.of(ORDER_TYPE));

            // then
            assertThat(event.aggregateIdField()).contains(aggregateIdField);
            assertThat(event.timestampField()).contains(timestampField);
            assertThat(event.sourceAggregate()).contains(ORDER_TYPE);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            DomainEvent ev1 = DomainEvent.of(ID, STRUCTURE, TRACE);
            DomainEvent ev2 = DomainEvent.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(ev1).isEqualTo(ev2);
            assertThat(ev1.hashCode()).isEqualTo(ev2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.OrderShipped");
            DomainEvent ev1 = DomainEvent.of(ID, STRUCTURE, TRACE);
            DomainEvent ev2 = DomainEvent.of(otherId, STRUCTURE, TRACE);

            // then
            assertThat(ev1).isNotEqualTo(ev2);
        }
    }
}
