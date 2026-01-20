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
 * Tests for {@link Identifier}.
 *
 * @since 4.1.0
 */
@DisplayName("Identifier")
class IdentifierTest {

    private static final TypeId ID = TypeId.of("com.example.OrderId");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.RECORD).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.IDENTIFIER, "explicit-identifier", "Has Identifier annotation");
    private static final TypeRef WRAPPED_TYPE = TypeRef.of("java.util.UUID");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            Identifier identifier = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);

            // then
            assertThat(identifier.id()).isEqualTo(ID);
            assertThat(identifier.structure()).isEqualTo(STRUCTURE);
            assertThat(identifier.classification()).isEqualTo(TRACE);
            assertThat(identifier.wrappedType()).isEqualTo(WRAPPED_TYPE);
        }

        @Test
        @DisplayName("should return IDENTIFIER kind")
        void shouldReturnIdentifierKind() {
            // when
            Identifier identifier = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);

            // then
            assertThat(identifier.kind()).isEqualTo(ArchKind.IDENTIFIER);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> Identifier.of(null, STRUCTURE, TRACE, WRAPPED_TYPE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> Identifier.of(ID, null, TRACE, WRAPPED_TYPE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> Identifier.of(ID, STRUCTURE, null, WRAPPED_TYPE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }

        @Test
        @DisplayName("should reject null wrapped type")
        void shouldRejectNullWrappedType() {
            assertThatThrownBy(() -> Identifier.of(ID, STRUCTURE, TRACE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("wrappedType");
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement DomainType interface")
        void shouldImplementDomainType() {
            // given
            Identifier identifier = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);

            // then
            assertThat(identifier).isInstanceOf(DomainType.class);
            assertThat(identifier).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            Identifier identifier = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);

            // then
            assertThat(identifier.qualifiedName()).isEqualTo("com.example.OrderId");
            assertThat(identifier.simpleName()).isEqualTo("OrderId");
            assertThat(identifier.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Wrapped Type")
    class WrappedTypeTests {

        @Test
        @DisplayName("should return the wrapped type")
        void shouldReturnWrappedType() {
            // given
            TypeRef uuidType = TypeRef.of("java.util.UUID");
            Identifier identifier = Identifier.of(ID, STRUCTURE, TRACE, uuidType);

            // then
            assertThat(identifier.wrappedType()).isEqualTo(uuidType);
            assertThat(identifier.wrappedType().qualifiedName()).isEqualTo("java.util.UUID");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            Identifier id1 = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);
            Identifier id2 = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);

            // then
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.CustomerId");
            Identifier id1 = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);
            Identifier id2 = Identifier.of(otherId, STRUCTURE, TRACE, WRAPPED_TYPE);

            // then
            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("Pattern Matching")
    class PatternMatching {

        @Test
        @DisplayName("should work with instanceof pattern matching")
        void shouldWorkWithInstanceofPatternMatching() {
            // given
            DomainType domain = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);

            // when / then
            if (domain instanceof Identifier id) {
                assertThat(id.wrappedType().simpleName()).isEqualTo("UUID");
            } else {
                throw new AssertionError("Expected Identifier type");
            }
        }

        @Test
        @DisplayName("should be castable to ArchType")
        void shouldBeCastableToArchType() {
            // given
            Identifier identifier = Identifier.of(ID, STRUCTURE, TRACE, WRAPPED_TYPE);

            // when
            ArchType archType = identifier;

            // then
            assertThat(archType.kind()).isEqualTo(ArchKind.IDENTIFIER);
        }
    }
}
