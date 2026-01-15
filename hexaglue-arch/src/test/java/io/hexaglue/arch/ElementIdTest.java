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

package io.hexaglue.arch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ElementId}.
 *
 * @since 4.0.0
 */
@DisplayName("ElementId")
class ElementIdTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create ElementId with valid qualified name")
        void shouldCreateWithValidQualifiedName() {
            // given
            String qualifiedName = "com.example.Order";

            // when
            ElementId id = ElementId.of(qualifiedName);

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
            ElementId id = ElementId.of(qualifiedName);

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
            ElementId id = ElementId.of(qualifiedName);

            // then
            assertThat(id.qualifiedName()).isEqualTo(qualifiedName);
            assertThat(id.simpleName()).isEqualTo("OrderLine");
            assertThat(id.packageName()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("should reject null qualified name")
        void shouldRejectNullQualifiedName() {
            assertThatThrownBy(() -> ElementId.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("qualifiedName");
        }

        @Test
        @DisplayName("should reject blank qualified name")
        void shouldRejectBlankQualifiedName() {
            assertThatThrownBy(() -> ElementId.of("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should reject empty qualified name")
        void shouldRejectEmptyQualifiedName() {
            assertThatThrownBy(() -> ElementId.of(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when qualified names match")
        void shouldBeEqualWhenQualifiedNamesMatch() {
            // given
            ElementId id1 = ElementId.of("com.example.Order");
            ElementId id2 = ElementId.of("com.example.Order");

            // then
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when qualified names differ")
        void shouldNotBeEqualWhenQualifiedNamesDiffer() {
            // given
            ElementId id1 = ElementId.of("com.example.Order");
            ElementId id2 = ElementId.of("com.example.Customer");

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
            ElementId a = ElementId.of("com.a.A");
            ElementId b = ElementId.of("com.b.B");

            // then
            assertThat(a).isLessThan(b);
            assertThat(b).isGreaterThan(a);
        }

        @Test
        @DisplayName("should be equal to itself in comparison")
        void shouldBeEqualToItselfInComparison() {
            // given
            ElementId id = ElementId.of("com.example.Order");

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
            ElementId id = ElementId.of("com.example.Order");

            // then
            assertThat(id.toString()).isEqualTo("com.example.Order");
        }
    }
}
