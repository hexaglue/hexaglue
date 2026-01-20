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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeStructure}.
 *
 * @since 4.1.0
 */
@DisplayName("TypeStructure")
class TypeStructureTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build minimal structure with nature")
        void shouldBuildMinimalStructure() {
            // when
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS).build();

            // then
            assertThat(structure.nature()).isEqualTo(TypeNature.CLASS);
            assertThat(structure.modifiers()).isEmpty();
            assertThat(structure.documentation()).isEmpty();
            assertThat(structure.superClass()).isEmpty();
            assertThat(structure.interfaces()).isEmpty();
            assertThat(structure.permittedSubtypes()).isEmpty();
            assertThat(structure.fields()).isEmpty();
            assertThat(structure.methods()).isEmpty();
            assertThat(structure.constructors()).isEmpty();
            assertThat(structure.annotations()).isEmpty();
            assertThat(structure.nestedTypes()).isEmpty();
        }

        @Test
        @DisplayName("should build complete structure")
        void shouldBuildCompleteStructure() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            Method getter = Method.of("getId", TypeRef.of("java.lang.Long"));
            Constructor ctor = Constructor.noArg();

            // when
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .modifiers(Set.of(Modifier.PUBLIC))
                    .documentation("An order entity")
                    .superClass(TypeRef.of("com.example.BaseEntity"))
                    .interfaces(List.of(TypeRef.of("java.io.Serializable")))
                    .fields(List.of(idField))
                    .methods(List.of(getter))
                    .constructors(List.of(ctor))
                    .annotations(List.of(Annotation.of("javax.persistence.Entity")))
                    .build();

            // then
            assertThat(structure.nature()).isEqualTo(TypeNature.CLASS);
            assertThat(structure.modifiers()).containsExactly(Modifier.PUBLIC);
            assertThat(structure.documentation()).contains("An order entity");
            assertThat(structure.superClass()).isPresent();
            assertThat(structure.interfaces()).hasSize(1);
            assertThat(structure.fields()).hasSize(1);
            assertThat(structure.methods()).hasSize(1);
            assertThat(structure.constructors()).hasSize(1);
            assertThat(structure.annotations()).hasSize(1);
        }

        @Test
        @DisplayName("should build sealed interface structure")
        void shouldBuildSealedInterfaceStructure() {
            // when
            TypeStructure structure = TypeStructure.builder(TypeNature.INTERFACE)
                    .modifiers(Set.of(Modifier.PUBLIC, Modifier.SEALED))
                    .permittedSubtypes(List.of(TypeRef.of("com.example.Entity"), TypeRef.of("com.example.ValueObject")))
                    .build();

            // then
            assertThat(structure.isSealed()).isTrue();
            assertThat(structure.permittedSubtypes()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Nature Checks")
    class NatureChecks {

        @Test
        @DisplayName("should identify class")
        void shouldIdentifyClass() {
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS).build();
            assertThat(structure.isClass()).isTrue();
            assertThat(structure.isInterface()).isFalse();
            assertThat(structure.isRecord()).isFalse();
        }

        @Test
        @DisplayName("should identify interface")
        void shouldIdentifyInterface() {
            TypeStructure structure =
                    TypeStructure.builder(TypeNature.INTERFACE).build();
            assertThat(structure.isClass()).isFalse();
            assertThat(structure.isInterface()).isTrue();
            assertThat(structure.isRecord()).isFalse();
        }

        @Test
        @DisplayName("should identify record")
        void shouldIdentifyRecord() {
            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD).build();
            assertThat(structure.isClass()).isFalse();
            assertThat(structure.isInterface()).isFalse();
            assertThat(structure.isRecord()).isTrue();
        }

        @Test
        @DisplayName("should identify sealed type")
        void shouldIdentifySealedType() {
            TypeStructure sealed = TypeStructure.builder(TypeNature.INTERFACE)
                    .modifiers(Set.of(Modifier.SEALED))
                    .build();
            TypeStructure notSealed =
                    TypeStructure.builder(TypeNature.INTERFACE).build();

            assertThat(sealed.isSealed()).isTrue();
            assertThat(notSealed.isSealed()).isFalse();
        }
    }

    @Nested
    @DisplayName("Field Access")
    class FieldAccess {

        @Test
        @DisplayName("should get field by name")
        void shouldGetFieldByName() {
            // given
            Field idField = Field.of("id", TypeRef.of("java.lang.Long"));
            Field nameField = Field.of("name", TypeRef.of("java.lang.String"));
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .fields(List.of(idField, nameField))
                    .build();

            // then
            assertThat(structure.getField("id")).contains(idField);
            assertThat(structure.getField("name")).contains(nameField);
            assertThat(structure.getField("unknown")).isEmpty();
        }

        @Test
        @DisplayName("should get fields with specific role")
        void shouldGetFieldsWithRole() {
            // given
            Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            Field nameField = Field.of("name", TypeRef.of("java.lang.String"));
            Field auditField = Field.builder("createdAt", TypeRef.of("java.time.Instant"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .fields(List.of(idField, nameField, auditField))
                    .build();

            // then
            assertThat(structure.getFieldsWithRole(FieldRole.IDENTITY)).containsExactly(idField);
            assertThat(structure.getFieldsWithRole(FieldRole.AUDIT)).containsExactly(auditField);
            assertThat(structure.getFieldsWithRole(FieldRole.COLLECTION)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable fields list")
        void shouldReturnImmutableFieldsList() {
            // given
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .fields(List.of(Field.of("id", TypeRef.of("java.lang.Long"))))
                    .build();

            // when/then
            assertThatThrownBy(() -> structure.fields().add(Field.of("extra", TypeRef.of("String"))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return immutable methods list")
        void shouldReturnImmutableMethodsList() {
            // given
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .methods(List.of(Method.of("getId", TypeRef.of("java.lang.Long"))))
                    .build();

            // when/then
            assertThatThrownBy(() -> structure.methods().add(Method.of("extra", TypeRef.of("String"))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return immutable modifiers set")
        void shouldReturnImmutableModifiersSet() {
            // given
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .modifiers(Set.of(Modifier.PUBLIC))
                    .build();

            // when/then
            assertThatThrownBy(() -> structure.modifiers().add(Modifier.STATIC))
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
            TypeStructure s1 = TypeStructure.builder(TypeNature.CLASS).build();
            TypeStructure s2 = TypeStructure.builder(TypeNature.CLASS).build();

            // then
            assertThat(s1).isEqualTo(s2);
            assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when natures differ")
        void shouldNotBeEqualWhenNaturesDiffer() {
            // given
            TypeStructure s1 = TypeStructure.builder(TypeNature.CLASS).build();
            TypeStructure s2 = TypeStructure.builder(TypeNature.RECORD).build();

            // then
            assertThat(s1).isNotEqualTo(s2);
        }
    }
}
