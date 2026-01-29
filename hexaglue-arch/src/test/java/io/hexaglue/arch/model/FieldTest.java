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

import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Field}.
 *
 * @since 4.1.0
 */
@DisplayName("Field")
class FieldTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create field with name and type")
        void shouldCreateWithNameAndType() {
            // given
            String name = "orderId";
            TypeRef type = TypeRef.of("com.example.OrderId");

            // when
            Field field = Field.of(name, type);

            // then
            assertThat(field.name()).isEqualTo(name);
            assertThat(field.type()).isEqualTo(type);
            assertThat(field.modifiers()).isEmpty();
            assertThat(field.annotations()).isEmpty();
            assertThat(field.documentation()).isEmpty();
            assertThat(field.wrappedType()).isEmpty();
            assertThat(field.elementType()).isEmpty();
            assertThat(field.roles()).isEmpty();
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            TypeRef type = TypeRef.of("java.lang.String");
            assertThatThrownBy(() -> Field.of(null, type))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            TypeRef type = TypeRef.of("java.lang.String");
            assertThatThrownBy(() -> Field.of("  ", type))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should reject null type")
        void shouldRejectNullType() {
            assertThatThrownBy(() -> Field.of("field", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("type");
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build field with all attributes")
        void shouldBuildWithAllAttributes() {
            // given
            TypeRef type = TypeRef.of("com.example.OrderId");
            TypeRef wrappedType = TypeRef.of("java.util.UUID");

            // when
            Field field = Field.builder("id", type)
                    .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                    .annotations(List.of(Annotation.of("javax.persistence.Id")))
                    .documentation("The order identifier")
                    .wrappedType(wrappedType)
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            // then
            assertThat(field.name()).isEqualTo("id");
            assertThat(field.type()).isEqualTo(type);
            assertThat(field.modifiers()).containsExactlyInAnyOrder(Modifier.PRIVATE, Modifier.FINAL);
            assertThat(field.annotations()).hasSize(1);
            assertThat(field.documentation()).contains("The order identifier");
            assertThat(field.wrappedType()).contains(wrappedType);
            assertThat(field.roles()).containsExactly(FieldRole.IDENTITY);
        }

        @Test
        @DisplayName("should build field with element type for collections")
        void shouldBuildWithElementType() {
            // given
            TypeRef listType = TypeRef.parameterized("java.util.List", List.of(TypeRef.of("com.example.Item")));
            TypeRef elementType = TypeRef.of("com.example.Item");

            // when
            Field field = Field.builder("items", listType)
                    .elementType(elementType)
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            // then
            assertThat(field.elementType()).contains(elementType);
            assertThat(field.hasRole(FieldRole.COLLECTION)).isTrue();
        }
    }

    @Nested
    @DisplayName("Role Checking")
    class RoleChecking {

        @Test
        @DisplayName("should check for specific role")
        void shouldCheckForSpecificRole() {
            // given
            Field field = Field.builder("id", TypeRef.of("com.example.OrderId"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            // then
            assertThat(field.hasRole(FieldRole.IDENTITY)).isTrue();
            assertThat(field.hasRole(FieldRole.COLLECTION)).isFalse();
        }

        @Test
        @DisplayName("should return true for identity field")
        void shouldReturnTrueForIdentityField() {
            // given
            Field field = Field.builder("id", TypeRef.of("com.example.OrderId"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            // then
            assertThat(field.isIdentity()).isTrue();
        }

        @Test
        @DisplayName("should return true for collection field")
        void shouldReturnTrueForCollectionField() {
            // given
            Field field = Field.builder("items", TypeRef.of("java.util.List"))
                    .roles(Set.of(FieldRole.COLLECTION))
                    .build();

            // then
            assertThat(field.isCollection()).isTrue();
        }
    }

    @Nested
    @DisplayName("Annotation Checking")
    class AnnotationChecking {

        @Test
        @DisplayName("should find annotation by qualified name")
        void shouldFindAnnotationByQualifiedName() {
            // given
            Field field = Field.builder("id", TypeRef.of("java.lang.Long"))
                    .annotations(List.of(Annotation.of("javax.persistence.Id")))
                    .build();

            // then
            assertThat(field.hasAnnotation("javax.persistence.Id")).isTrue();
            assertThat(field.hasAnnotation("javax.persistence.Column")).isFalse();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable modifiers set")
        void shouldReturnImmutableModifiersSet() {
            // given
            Set<Modifier> modifiers = new java.util.HashSet<>(Set.of(Modifier.PRIVATE));
            Field field = new Field(
                    "id",
                    TypeRef.of("java.lang.Long"),
                    modifiers,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Set.of(),
                    Optional.empty());

            // when/then
            assertThatThrownBy(() -> field.modifiers().add(Modifier.FINAL))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return immutable roles set")
        void shouldReturnImmutableRolesSet() {
            // given
            Field field = Field.builder("id", TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();

            // when/then
            assertThatThrownBy(() -> field.roles().add(FieldRole.AUDIT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            Field f1 = Field.of("orderId", TypeRef.of("com.example.OrderId"));
            Field f2 = Field.of("orderId", TypeRef.of("com.example.OrderId"));

            // then
            assertThat(f1).isEqualTo(f2);
            assertThat(f1.hashCode()).isEqualTo(f2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when names differ")
        void shouldNotBeEqualWhenNamesDiffer() {
            // given
            TypeRef type = TypeRef.of("java.lang.String");
            Field f1 = Field.of("name", type);
            Field f2 = Field.of("title", type);

            // then
            assertThat(f1).isNotEqualTo(f2);
        }
    }
}
