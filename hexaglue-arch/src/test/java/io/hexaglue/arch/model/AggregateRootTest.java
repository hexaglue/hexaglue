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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateRoot}.
 *
 * @since 4.1.0
 */
@DisplayName("AggregateRoot")
class AggregateRootTest {

    private static final TypeId ID = TypeId.of("com.example.Order");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.CLASS).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.AGGREGATE_ROOT, "explicit-aggregate-root", "Has AggregateRoot annotation");
    private static final Field IDENTITY_FIELD = Field.builder("id", TypeRef.of("com.example.OrderId"))
            .roles(Set.of(FieldRole.IDENTITY))
            .build();

    @Nested
    @DisplayName("Builder Construction")
    class BuilderConstruction {

        @Test
        @DisplayName("should create with required fields only")
        void shouldCreateWithRequiredFieldsOnly() {
            // when
            AggregateRoot agg =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();

            // then
            assertThat(agg.id()).isEqualTo(ID);
            assertThat(agg.structure()).isEqualTo(STRUCTURE);
            assertThat(agg.classification()).isEqualTo(TRACE);
            assertThat(agg.identityField()).isEqualTo(IDENTITY_FIELD);
            assertThat(agg.effectiveIdentityType()).isEqualTo(IDENTITY_FIELD.type());
            assertThat(agg.entities()).isEmpty();
            assertThat(agg.valueObjects()).isEmpty();
            assertThat(agg.domainEvents()).isEmpty();
            assertThat(agg.drivenPort()).isEmpty();
            assertThat(agg.invariants()).isEmpty();
        }

        @Test
        @DisplayName("should create with all optional fields")
        void shouldCreateWithAllOptionalFields() {
            // given
            TypeRef entity = TypeRef.of("com.example.OrderLine");
            TypeRef valueObject = TypeRef.of("com.example.Money");
            TypeRef event = TypeRef.of("com.example.OrderCreated");
            TypeRef port = TypeRef.of("com.example.OrderRepository");
            Invariant invariant = Invariant.of("orderMustHaveItems", "An order must have items");
            TypeRef customIdType = TypeRef.of("java.util.UUID");

            // when
            AggregateRoot agg = AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD)
                    .effectiveIdentityType(customIdType)
                    .entities(List.of(entity))
                    .valueObjects(List.of(valueObject))
                    .domainEvents(List.of(event))
                    .drivenPort(port)
                    .invariants(List.of(invariant))
                    .build();

            // then
            assertThat(agg.effectiveIdentityType()).isEqualTo(customIdType);
            assertThat(agg.entities()).containsExactly(entity);
            assertThat(agg.valueObjects()).containsExactly(valueObject);
            assertThat(agg.domainEvents()).containsExactly(event);
            assertThat(agg.drivenPort()).isPresent().contains(port);
            assertThat(agg.invariants()).containsExactly(invariant);
        }

        @Test
        @DisplayName("should return AGGREGATE_ROOT kind")
        void shouldReturnAggregateRootKind() {
            // when
            AggregateRoot agg =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();

            // then
            assertThat(agg.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should reject null id in builder")
        void shouldRejectNullIdInBuilder() {
            assertThatThrownBy(() -> AggregateRoot.builder(null, STRUCTURE, TRACE, IDENTITY_FIELD))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure in builder")
        void shouldRejectNullStructureInBuilder() {
            assertThatThrownBy(() -> AggregateRoot.builder(ID, null, TRACE, IDENTITY_FIELD))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification in builder")
        void shouldRejectNullClassificationInBuilder() {
            assertThatThrownBy(() -> AggregateRoot.builder(ID, STRUCTURE, null, IDENTITY_FIELD))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }

        @Test
        @DisplayName("should reject null identity field in builder")
        void shouldRejectNullIdentityFieldInBuilder() {
            assertThatThrownBy(() -> AggregateRoot.builder(ID, STRUCTURE, TRACE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("identityField");
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement DomainType interface")
        void shouldImplementDomainType() {
            // given
            AggregateRoot agg =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();

            // then
            assertThat(agg).isInstanceOf(DomainType.class);
            assertThat(agg).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            AggregateRoot agg =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();

            // then
            assertThat(agg.qualifiedName()).isEqualTo("com.example.Order");
            assertThat(agg.simpleName()).isEqualTo("Order");
            assertThat(agg.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("entities list should be immutable")
        void entitiesListShouldBeImmutable() {
            // given
            AggregateRoot agg = AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD)
                    .entities(List.of(TypeRef.of("com.example.OrderLine")))
                    .build();

            // then
            assertThatThrownBy(() -> agg.entities().add(TypeRef.of("com.example.Other")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("valueObjects list should be immutable")
        void valueObjectsListShouldBeImmutable() {
            // given
            AggregateRoot agg = AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD)
                    .valueObjects(List.of(TypeRef.of("com.example.Money")))
                    .build();

            // then
            assertThatThrownBy(() -> agg.valueObjects().add(TypeRef.of("com.example.Other")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("domainEvents list should be immutable")
        void domainEventsListShouldBeImmutable() {
            // given
            AggregateRoot agg = AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD)
                    .domainEvents(List.of(TypeRef.of("com.example.OrderCreated")))
                    .build();

            // then
            assertThatThrownBy(() -> agg.domainEvents().add(TypeRef.of("com.example.Other")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("invariants list should be immutable")
        void invariantsListShouldBeImmutable() {
            // given
            AggregateRoot agg = AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD)
                    .invariants(List.of(Invariant.of("test", "Test invariant")))
                    .build();

            // then
            assertThatThrownBy(() -> agg.invariants().add(Invariant.of("other", "Other")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("hasDrivenPort should return true when port present")
        void hasDrivenPortShouldReturnTrueWhenPresent() {
            // given
            AggregateRoot agg = AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD)
                    .drivenPort(TypeRef.of("com.example.OrderRepository"))
                    .build();

            // then
            assertThat(agg.hasDrivenPort()).isTrue();
        }

        @Test
        @DisplayName("hasDrivenPort should return false when port absent")
        void hasDrivenPortShouldReturnFalseWhenAbsent() {
            // given
            AggregateRoot agg =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();

            // then
            assertThat(agg.hasDrivenPort()).isFalse();
        }

        @Test
        @DisplayName("hasInvariants should return true when invariants present")
        void hasInvariantsShouldReturnTrueWhenPresent() {
            // given
            AggregateRoot agg = AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD)
                    .invariants(List.of(Invariant.of("test", "Test")))
                    .build();

            // then
            assertThat(agg.hasInvariants()).isTrue();
        }

        @Test
        @DisplayName("hasInvariants should return false when invariants absent")
        void hasInvariantsShouldReturnFalseWhenAbsent() {
            // given
            AggregateRoot agg =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();

            // then
            assertThat(agg.hasInvariants()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            AggregateRoot agg1 =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();
            AggregateRoot agg2 =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();

            // then
            assertThat(agg1).isEqualTo(agg2);
            assertThat(agg1.hashCode()).isEqualTo(agg2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.Customer");
            AggregateRoot agg1 =
                    AggregateRoot.builder(ID, STRUCTURE, TRACE, IDENTITY_FIELD).build();
            AggregateRoot agg2 = AggregateRoot.builder(otherId, STRUCTURE, TRACE, IDENTITY_FIELD)
                    .build();

            // then
            assertThat(agg1).isNotEqualTo(agg2);
        }
    }
}
