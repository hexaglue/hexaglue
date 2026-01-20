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

import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Parameter}.
 *
 * @since 4.1.0
 */
@DisplayName("Parameter")
class ParameterTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create parameter with name and type")
        void shouldCreateWithNameAndType() {
            // given
            String name = "orderId";
            TypeRef type = TypeRef.of("com.example.OrderId");

            // when
            Parameter param = Parameter.of(name, type);

            // then
            assertThat(param.name()).isEqualTo(name);
            assertThat(param.type()).isEqualTo(type);
            assertThat(param.annotations()).isEmpty();
        }

        @Test
        @DisplayName("should create parameter with annotations")
        void shouldCreateWithAnnotations() {
            // given
            String name = "orderId";
            TypeRef type = TypeRef.of("com.example.OrderId");
            List<Annotation> annotations = List.of(Annotation.of("javax.annotation.Nonnull"));

            // when
            Parameter param = new Parameter(name, type, annotations);

            // then
            assertThat(param.name()).isEqualTo(name);
            assertThat(param.type()).isEqualTo(type);
            assertThat(param.annotations()).hasSize(1);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            TypeRef type = TypeRef.of("java.lang.String");
            assertThatThrownBy(() -> Parameter.of(null, type))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            TypeRef type = TypeRef.of("java.lang.String");
            assertThatThrownBy(() -> Parameter.of("  ", type))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should reject null type")
        void shouldRejectNullType() {
            assertThatThrownBy(() -> Parameter.of("orderId", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("type");
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable annotations list")
        void shouldReturnImmutableAnnotationsList() {
            // given
            List<Annotation> annotations =
                    new java.util.ArrayList<>(List.of(Annotation.of("javax.annotation.Nonnull")));
            Parameter param = new Parameter("orderId", TypeRef.of("com.example.OrderId"), annotations);

            // when/then
            assertThatThrownBy(() -> param.annotations().add(Annotation.of("Extra")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should not be affected by original list modifications")
        void shouldNotBeAffectedByOriginalListModifications() {
            // given
            List<Annotation> annotations =
                    new java.util.ArrayList<>(List.of(Annotation.of("javax.annotation.Nonnull")));
            Parameter param = new Parameter("orderId", TypeRef.of("com.example.OrderId"), annotations);

            // when
            annotations.add(Annotation.of("Extra"));

            // then
            assertThat(param.annotations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Annotation Checking")
    class AnnotationChecking {

        @Test
        @DisplayName("should find annotation by qualified name")
        void shouldFindAnnotationByQualifiedName() {
            // given
            Parameter param = new Parameter(
                    "orderId", TypeRef.of("com.example.OrderId"), List.of(Annotation.of("javax.annotation.Nonnull")));

            // then
            assertThat(param.hasAnnotation("javax.annotation.Nonnull")).isTrue();
            assertThat(param.hasAnnotation("javax.annotation.Nullable")).isFalse();
        }

        @Test
        @DisplayName("should return false for empty annotations")
        void shouldReturnFalseForEmptyAnnotations() {
            // given
            Parameter param = Parameter.of("orderId", TypeRef.of("com.example.OrderId"));

            // then
            assertThat(param.hasAnnotation("javax.annotation.Nonnull")).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when name, type, and annotations match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            Parameter p1 = Parameter.of("orderId", TypeRef.of("com.example.OrderId"));
            Parameter p2 = Parameter.of("orderId", TypeRef.of("com.example.OrderId"));

            // then
            assertThat(p1).isEqualTo(p2);
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when names differ")
        void shouldNotBeEqualWhenNamesDiffer() {
            // given
            TypeRef type = TypeRef.of("com.example.OrderId");
            Parameter p1 = Parameter.of("orderId", type);
            Parameter p2 = Parameter.of("customerId", type);

            // then
            assertThat(p1).isNotEqualTo(p2);
        }
    }
}
