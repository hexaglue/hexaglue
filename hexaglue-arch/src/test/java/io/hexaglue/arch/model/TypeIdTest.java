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

import io.hexaglue.arch.ElementId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeId}.
 *
 * @since 4.1.0
 */
@DisplayName("TypeId")
class TypeIdTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create TypeId with valid qualified name")
        void shouldCreateWithValidQualifiedName() {
            // given
            String qualifiedName = "com.example.Order";

            // when
            TypeId id = TypeId.of(qualifiedName);

            // then
            assertThat(id.qualifiedName()).isEqualTo(qualifiedName);
            assertThat(id.simpleName()).isEqualTo("Order");
            assertThat(id.packageName()).isEqualTo("com.example");
        }

        @Test
        @DisplayName("should handle simple name without package")
        void shouldHandleSimpleNameWithoutPackage() {
            // given
            String qualifiedName = "Order";

            // when
            TypeId id = TypeId.of(qualifiedName);

            // then
            assertThat(id.qualifiedName()).isEqualTo("Order");
            assertThat(id.simpleName()).isEqualTo("Order");
            assertThat(id.packageName()).isEmpty();
        }

        @Test
        @DisplayName("should handle nested class qualified name")
        void shouldHandleNestedClassQualifiedName() {
            // given
            String qualifiedName = "com.example.Order$OrderLine";

            // when
            TypeId id = TypeId.of(qualifiedName);

            // then
            assertThat(id.qualifiedName()).isEqualTo(qualifiedName);
            assertThat(id.simpleName()).isEqualTo("OrderLine");
            assertThat(id.packageName()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("should reject null qualified name")
        void shouldRejectNullQualifiedName() {
            assertThatThrownBy(() -> TypeId.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("qualifiedName");
        }

        @Test
        @DisplayName("should reject blank qualified name")
        void shouldRejectBlankQualifiedName() {
            assertThatThrownBy(() -> TypeId.of("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should reject empty qualified name")
        void shouldRejectEmptyQualifiedName() {
            assertThatThrownBy(() -> TypeId.of(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }
    }

    @Nested
    @DisplayName("ElementId Conversion")
    class ElementIdConversion {

        @Test
        @DisplayName("should create TypeId from ElementId")
        void shouldCreateFromElementId() {
            // given
            ElementId elementId = ElementId.of("com.example.Order");

            // when
            TypeId typeId = TypeId.fromElementId(elementId);

            // then
            assertThat(typeId.qualifiedName()).isEqualTo(elementId.qualifiedName());
        }

        @Test
        @DisplayName("should convert TypeId to ElementId")
        void shouldConvertToElementId() {
            // given
            TypeId typeId = TypeId.of("com.example.Order");

            // when
            ElementId elementId = typeId.toElementId();

            // then
            assertThat(elementId.qualifiedName()).isEqualTo(typeId.qualifiedName());
        }

        @Test
        @DisplayName("should round-trip conversion")
        void shouldRoundTripConversion() {
            // given
            String qualifiedName = "com.example.Order";
            ElementId original = ElementId.of(qualifiedName);

            // when
            TypeId typeId = TypeId.fromElementId(original);
            ElementId converted = typeId.toElementId();

            // then
            assertThat(converted).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when qualified names match")
        void shouldBeEqualWhenQualifiedNamesMatch() {
            // given
            TypeId id1 = TypeId.of("com.example.Order");
            TypeId id2 = TypeId.of("com.example.Order");

            // then
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when qualified names differ")
        void shouldNotBeEqualWhenQualifiedNamesDiffer() {
            // given
            TypeId id1 = TypeId.of("com.example.Order");
            TypeId id2 = TypeId.of("com.example.Customer");

            // then
            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("Comparison")
    class Comparison {

        @Test
        @DisplayName("should compare by qualified name alphabetically")
        void shouldCompareAlphabetically() {
            // given
            TypeId a = TypeId.of("com.a.A");
            TypeId b = TypeId.of("com.b.B");

            // then
            assertThat(a).isLessThan(b);
            assertThat(b).isGreaterThan(a);
        }

        @Test
        @DisplayName("should be equal to itself in comparison")
        void shouldBeEqualToItselfInComparison() {
            // given
            TypeId id = TypeId.of("com.example.Order");

            // then
            assertThat(id.compareTo(id)).isZero();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringBehavior {

        @Test
        @DisplayName("should return qualified name as string")
        void shouldReturnQualifiedNameAsString() {
            // given
            TypeId id = TypeId.of("com.example.Order");

            // then
            assertThat(id.toString()).isEqualTo("com.example.Order");
        }
    }
}
