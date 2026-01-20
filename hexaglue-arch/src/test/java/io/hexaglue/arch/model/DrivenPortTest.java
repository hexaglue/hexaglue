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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DrivenPort}.
 *
 * @since 4.1.0
 */
@DisplayName("DrivenPort")
class DrivenPortTest {

    private static final TypeId ID = TypeId.of("com.example.OrderRepository");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.INTERFACE).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.DRIVEN_PORT, "explicit-driven-port", "Has repository annotation");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with port type only")
        void shouldCreateWithPortTypeOnly() {
            // when
            DrivenPort port = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.REPOSITORY);

            // then
            assertThat(port.id()).isEqualTo(ID);
            assertThat(port.structure()).isEqualTo(STRUCTURE);
            assertThat(port.classification()).isEqualTo(TRACE);
            assertThat(port.portType()).isEqualTo(DrivenPortType.REPOSITORY);
            assertThat(port.managedAggregate()).isEmpty();
        }

        @Test
        @DisplayName("should create repository with managed aggregate")
        void shouldCreateRepositoryWithManagedAggregate() {
            // given
            TypeRef aggregate = TypeRef.of("com.example.Order");

            // when
            DrivenPort port = DrivenPort.repository(ID, STRUCTURE, TRACE, aggregate);

            // then
            assertThat(port.portType()).isEqualTo(DrivenPortType.REPOSITORY);
            assertThat(port.managedAggregate()).isPresent().contains(aggregate);
        }

        @Test
        @DisplayName("should return DRIVEN_PORT kind")
        void shouldReturnDrivenPortKind() {
            // when
            DrivenPort port = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.GATEWAY);

            // then
            assertThat(port.kind()).isEqualTo(ArchKind.DRIVEN_PORT);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> DrivenPort.of(null, STRUCTURE, TRACE, DrivenPortType.REPOSITORY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> DrivenPort.of(ID, null, TRACE, DrivenPortType.REPOSITORY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> DrivenPort.of(ID, STRUCTURE, null, DrivenPortType.REPOSITORY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }

        @Test
        @DisplayName("should reject null port type")
        void shouldRejectNullPortType() {
            assertThatThrownBy(() -> DrivenPort.of(ID, STRUCTURE, TRACE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("portType");
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement PortType interface")
        void shouldImplementPortType() {
            // given
            DrivenPort port = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.REPOSITORY);

            // then
            assertThat(port).isInstanceOf(PortType.class);
            assertThat(port).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            DrivenPort port = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.REPOSITORY);

            // then
            assertThat(port.qualifiedName()).isEqualTo("com.example.OrderRepository");
            assertThat(port.simpleName()).isEqualTo("OrderRepository");
            assertThat(port.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Port Type")
    class PortTypeTests {

        @Test
        @DisplayName("should correctly identify REPOSITORY type")
        void shouldIdentifyRepositoryType() {
            // given
            DrivenPort port = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.REPOSITORY);

            // then
            assertThat(port.isRepository()).isTrue();
            assertThat(port.isGateway()).isFalse();
            assertThat(port.isEventPublisher()).isFalse();
        }

        @Test
        @DisplayName("should correctly identify GATEWAY type")
        void shouldIdentifyGatewayType() {
            // given
            TypeId gatewayId = TypeId.of("com.example.PaymentGateway");
            DrivenPort port = DrivenPort.of(gatewayId, STRUCTURE, TRACE, DrivenPortType.GATEWAY);

            // then
            assertThat(port.isRepository()).isFalse();
            assertThat(port.isGateway()).isTrue();
            assertThat(port.isEventPublisher()).isFalse();
        }

        @Test
        @DisplayName("should correctly identify EVENT_PUBLISHER type")
        void shouldIdentifyEventPublisherType() {
            // given
            TypeId publisherId = TypeId.of("com.example.EventPublisher");
            DrivenPort port = DrivenPort.of(publisherId, STRUCTURE, TRACE, DrivenPortType.EVENT_PUBLISHER);

            // then
            assertThat(port.isRepository()).isFalse();
            assertThat(port.isGateway()).isFalse();
            assertThat(port.isEventPublisher()).isTrue();
        }
    }

    @Nested
    @DisplayName("Managed Aggregate")
    class ManagedAggregate {

        @Test
        @DisplayName("hasAggregate should return true when aggregate present")
        void hasAggregateShouldReturnTrueWhenPresent() {
            // given
            TypeRef aggregate = TypeRef.of("com.example.Order");
            DrivenPort port = DrivenPort.repository(ID, STRUCTURE, TRACE, aggregate);

            // then
            assertThat(port.hasAggregate()).isTrue();
        }

        @Test
        @DisplayName("hasAggregate should return false when aggregate absent")
        void hasAggregateShouldReturnFalseWhenAbsent() {
            // given
            DrivenPort port = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.REPOSITORY);

            // then
            assertThat(port.hasAggregate()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            DrivenPort p1 = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.REPOSITORY);
            DrivenPort p2 = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.REPOSITORY);

            // then
            assertThat(p1).isEqualTo(p2);
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when port types differ")
        void shouldNotBeEqualWhenPortTypesDiffer() {
            // given
            DrivenPort p1 = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.REPOSITORY);
            DrivenPort p2 = DrivenPort.of(ID, STRUCTURE, TRACE, DrivenPortType.GATEWAY);

            // then
            assertThat(p1).isNotEqualTo(p2);
        }
    }
}
